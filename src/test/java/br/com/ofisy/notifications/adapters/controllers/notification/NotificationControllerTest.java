package br.com.ofisy.notifications.adapters.controllers.notification;
import br.com.ofisy.notifications.application.notification.findstockbyid.FindStockNotificationByIdService;
import br.com.ofisy.notifications.application.notification.findserviceorderbyid.FindServiceOrderNotificationByIdService;
import br.com.ofisy.notifications.application.notification.markstockasread.MarkStockNotificationAsReadService;
import br.com.ofisy.notifications.application.notification.markserviceorderasread.MarkServiceOrderNotificationAsReadService;
import br.com.ofisy.notifications.application.notification.findallstock.FindAllStockNotificationsService;
import br.com.ofisy.notifications.application.notification.findunreadstock.FindUnreadStockNotificationsService;
import br.com.ofisy.notifications.application.notification.findallserviceorder.FindAllServiceOrderNotificationsService;
import br.com.ofisy.notifications.application.notification.findunreadserviceorder.FindUnreadServiceOrderNotificationsService;
import br.com.ofisy.notifications.config.NotificationExceptionHandler;
import br.com.ofisy.notifications.domain.notification.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class NotificationControllerTest {
    @ParameterizedTest
    @CsvSource({"stock", "stock/unread", "service-orders", "service-orders/unread"})
    void invalidLimitsReturn400BeforeCallingRepository(String route) throws Exception {
        var repository=mock(NotificationRepository.class);
        var mvc=MockMvcBuilders.standaloneSetup(new NotificationController(new FindStockNotificationByIdService(repository), new FindServiceOrderNotificationByIdService(repository), new MarkStockNotificationAsReadService(repository), new MarkServiceOrderNotificationAsReadService(repository), new FindAllStockNotificationsService(repository), new FindUnreadStockNotificationsService(repository), new FindAllServiceOrderNotificationsService(repository), new FindUnreadServiceOrderNotificationsService(repository))).setControllerAdvice(new NotificationExceptionHandler()).build();
        for (String limit : new String[] {"0", "-1", "101"}) {
            mvc.perform(get("/api/v1/notifications/"+route).param("limit", limit))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.title").value("Paginação inválida"));
        }
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({"stock,LOW_STOCK,false","stock/unread,LOW_STOCK,true","service-orders,QUOTE_GENERATED,false","service-orders/unread,QUOTE_GENERATED,true"})
    void listRoutesKeepTypeAndCursorContract(String route,NotificationType type,boolean unread) throws Exception {
        var repository=mock(NotificationRepository.class);
        var mvc=MockMvcBuilders.standaloneSetup(new NotificationController(new FindStockNotificationByIdService(repository), new FindServiceOrderNotificationByIdService(repository), new MarkStockNotificationAsReadService(repository), new MarkServiceOrderNotificationAsReadService(repository), new FindAllStockNotificationsService(repository), new FindUnreadStockNotificationsService(repository), new FindAllServiceOrderNotificationsService(repository), new FindUnreadServiceOrderNotificationsService(repository))).setControllerAdvice(new NotificationExceptionHandler()).build();
        var cursor="opaque-input-token";
        var page=new PaginatedResult<Notification>(List.of(),"opaque-next-token");
        if(unread) when(repository.findUnreadByType(eq(type),eq(10),eq(cursor))).thenReturn(page);
        else when(repository.findAllByType(eq(type),eq(10),eq(cursor))).thenReturn(page);
        mvc.perform(get("/api/v1/notifications/"+route).param("limit","10").param("nextToken",cursor))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items").isArray()).andExpect(jsonPath("$.nextToken").value("opaque-next-token"));
    }
    @ParameterizedTest
    @CsvSource({"stock,LOW_STOCK","service-orders,QUOTE_GENERATED"})
    void correctTypeSucceedsAndWrongOrMissingTypeReturnsIdentical404WithoutUpdating(String route,NotificationType type) throws Exception {
        var repository=mock(NotificationRepository.class);
        var mvc=MockMvcBuilders.standaloneSetup(new NotificationController(new FindStockNotificationByIdService(repository), new FindServiceOrderNotificationByIdService(repository), new MarkStockNotificationAsReadService(repository), new MarkServiceOrderNotificationAsReadService(repository), new FindAllStockNotificationsService(repository), new FindUnreadStockNotificationsService(repository), new FindAllServiceOrderNotificationsService(repository), new FindUnreadServiceOrderNotificationsService(repository))).setControllerAdvice(new NotificationExceptionHandler()).build();
        var id=UUID.randomUUID();var n=Notification.create(id,type,"Mensagem",UUID.randomUUID());
        when(repository.findById(id)).thenReturn(Optional.of(n));when(repository.save(n)).thenReturn(n);
        var path="/api/v1/notifications/"+route+"/"+id;
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath(type==NotificationType.LOW_STOCK?"$.stockId":"$.quoteId").value(n.getReferenceId().toString()));
        mvc.perform(patch(path+"/read")).andExpect(status().isOk()).andExpect(jsonPath("$.read").value(true));
        clearInvocations(repository);
        var wrong=Notification.create(id,type==NotificationType.LOW_STOCK?NotificationType.QUOTE_GENERATED:NotificationType.LOW_STOCK,"Mensagem",UUID.randomUUID());
        when(repository.findById(id)).thenReturn(Optional.of(wrong));
        mvc.perform(get(path)).andExpect(status().isNotFound()).andExpect(jsonPath("$.title").value("Notificação não encontrada"));
        mvc.perform(patch(path+"/read")).andExpect(status().isNotFound());
        assertThat(wrong.isRead()).isFalse();verify(repository,never()).save(any());
        when(repository.findById(id)).thenReturn(Optional.empty());
        mvc.perform(get(path)).andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Notificação não encontrada: "+id));
        mvc.perform(patch(path+"/read")).andExpect(status().isNotFound());
    }
}
