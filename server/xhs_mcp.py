"""Read-only adapter for the local xiaohongshu-mcp Streamable HTTP endpoint."""
import base64
import json
import os
import re
from urllib.error import URLError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen, build_opener, HTTPRedirectHandler

MCP_URL = "http://127.0.0.1:18060/mcp"
NOTE_ID = re.compile(r"[0-9a-f]{24}")
MAX_IMAGE_BYTES = 1_500_000


class XhsUnavailable(Exception):
    pass


def call_read_tool(name, arguments):
    headers = {"Accept": "application/json, text/event-stream", "Content-Type": "application/json"}
    if token := os.getenv("COOKX_XHS_MCP_TOKEN"):
        headers["Authorization"] = "Bearer " + token
    data = {"jsonrpc": "2.0", "id": 1, "method": "tools/call",
            "params": {"name": name, "arguments": arguments}}
    try:
        # The pinned upstream runs in stateless JSON mode.
        with urlopen(Request(MCP_URL, json.dumps(data).encode(), headers), timeout=28) as response:
            result = json.load(response)["result"]
        content = next(part["text"] for part in result["content"] if part["type"] == "text")
        if result.get("isError"):
            if any(word in content for word in ("登录", "login")):
                raise XhsUnavailable("小红书会话需要重新登录。也可以粘贴正文生成草稿。")
            raise ValueError
        return json.loads(content)
    except (URLError, TimeoutError, KeyError, TypeError, ValueError, StopIteration) as exc:
        raise XhsUnavailable("暂时读不到小红书，可以重试或粘贴正文。") from exc


def search(keyword, excluded):
    result = call_read_tool("search_feeds", {"keyword": keyword, "filters": {"note_type": "图文"}})
    candidates = []
    for feed in result.get("feeds", []):
        card = feed.get("noteCard") or {}
        note_id, token = feed.get("id", ""), feed.get("xsecToken")
        if (NOTE_ID.fullmatch(note_id) and token and note_id not in excluded
                and card.get("type") != "video" and card.get("displayTitle")):
            candidates.append((note_id, token, card["displayTitle"][:160]))
    return candidates[:12]


def detail(candidate):
    note_id, token, _ = candidate
    result = call_read_tool("get_feed_detail", {"feed_id": note_id, "xsec_token": token})
    note = result.get("note") or (result.get("data") or {}).get("note")
    if not note or note.get("noteId") != note_id:
        raise XhsUnavailable("这篇帖子暂时无法读取。")
    user = note.get("user") or {}
    return {
        "source": {"platform": "xiaohongshu", "url": "https://www.xiaohongshu.com/explore/" + note_id,
                   "title": note.get("title"), "author": user.get("nickname") or user.get("nickName"),
                   "noteId": note_id},
        "body": (note.get("desc") or "")[:6000],
        "images": note.get("imageList") or [],
    }


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def source_images(images):
    """Fetch a bounded number of approved CDN images without passing signed URLs upstream."""
    client = build_opener(NoRedirect())
    for index, item in enumerate(images[:4], start=1):
        url = item.get("urlDefault") or item.get("urlPre") or ""
        parsed = urlsplit(url)
        host = parsed.hostname or ""
        if (parsed.scheme != "https" or not host.endswith(".xhscdn.com")
                or parsed.username or parsed.password or parsed.netloc != host):
            continue
        try:
            with client.open(Request(url, headers={"User-Agent": "Mozilla/5.0"}), timeout=4) as response:
                media = response.headers.get_content_type()
                data = response.read(MAX_IMAGE_BYTES + 1)
            if media in ("image/jpeg", "image/png", "image/webp") and 0 < len(data) <= MAX_IMAGE_BYTES:
                yield index, f"data:{media};base64," + base64.b64encode(data).decode()
        except (URLError, TimeoutError, ValueError):
            continue
