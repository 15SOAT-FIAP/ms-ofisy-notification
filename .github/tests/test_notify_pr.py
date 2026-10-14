"""Exercise the actual inline workflow without contacting Discord."""
import json
import os
from pathlib import Path
import textwrap
import unittest
from unittest.mock import patch


class NotifyPrTest(unittest.TestCase):
    def setUp(self):
        workflow = (Path(__file__).parents[1] / "workflows/notify-pr.yml").read_text(encoding="utf-8")
        script = textwrap.dedent(workflow.split("        run: |\n", 1)[1])
        self.assertNotIn("${{", script)
        namespace = {"__name__": "workflow_test"}
        exec(compile(script, "notify-pr.yml", "exec"), namespace)
        self.main = namespace["main"]
        self.env = {
            "DISCORD_WEBHOOK_URL": "https://example.invalid/webhook-secret",
            "PR_ACTION": "opened", "PR_MERGED": "false",
            "PR_TITLE": 'Fix "quotes" \\ newline\n$(touch injected) `whoami` 🚀',
            "PR_URL": "https://github.com/org/repo/pull/1", "PR_REPOSITORY": "org/repo",
            "PR_HEAD": 'feature/$(touch injected)"', "PR_BASE": "master", "PR_AUTHOR": "user",
        }

    def test_untrusted_values_remain_json_data(self):
        with patch.dict(os.environ, self.env, clear=True), patch("urllib.request.urlopen") as send:
            send.return_value.__enter__.return_value.status = 204
            self.main()
            request = send.call_args.args[0]
            embed = json.loads(request.data)["embeds"][0]
            self.assertIn(self.env["PR_TITLE"], embed["description"])
            self.assertIn(self.env["PR_HEAD"], embed["fields"][1]["value"])
            self.assertEqual(request.method, "POST")
            self.assertEqual(send.call_args.kwargs["timeout"], 15)

    def test_event_variants(self):
        for action, merged, color in [("reopened", "false", 3447003), ("closed", "true", 3066993), ("closed", "false", 15158332)]:
            with self.subTest(action=action, merged=merged), patch.dict(os.environ, self.env | {"PR_ACTION": action, "PR_MERGED": merged}, clear=True), patch("urllib.request.urlopen") as send:
                send.return_value.__enter__.return_value.status = 204
                self.main()
                self.assertEqual(json.loads(send.call_args.args[0].data)["embeds"][0]["color"], color)

    def test_missing_secret_skips_network(self):
        with patch.dict(os.environ, {}, clear=True), patch("urllib.request.urlopen") as send:
            self.main()
            send.assert_not_called()

    def test_http_or_network_failure_fails_without_leaking_secret(self):
        with patch.dict(os.environ, self.env, clear=True), patch("urllib.request.urlopen", side_effect=OSError(self.env["DISCORD_WEBHOOK_URL"])):
            with self.assertRaises(SystemExit) as error:
                self.main()
            self.assertEqual(str(error.exception), "Discord notification failed.")


if __name__ == "__main__":
    unittest.main()
