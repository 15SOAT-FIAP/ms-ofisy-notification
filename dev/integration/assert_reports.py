"""Fail CI if required integration tests did not run, failed, or were skipped."""
from pathlib import Path
import xml.etree.ElementTree as ET

reports = list(Path("target/surefire-reports").glob("TEST-*.xml"))
assert reports, "Surefire reports are missing"
required = {"DynamoDbNotificationAtomicInsertTest", "DynamoDbPaginationIntegrationTest",
            "NotificationArchitectureIntegrationTest"}
total = 0
for report in reports:
    suite = ET.parse(report).getroot()
    assert all(int(suite.get(key, "0")) == 0 for key in ("failures", "errors", "skipped")), report
    executed = int(suite.get("tests", "0"))
    total += executed
    if executed:
        required.discard(suite.get("name", "").rsplit(".", 1)[-1])
assert not required, "Required integrations did not execute: " + str(sorted(required))
print(f"PASS: {total} tests, no failures/errors/skips; all required integrations executed")
