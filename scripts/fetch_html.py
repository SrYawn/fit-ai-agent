#!/usr/bin/env python3
"""
网页抓取脚本（增强版）。

能力:
1. 单页抓取或批量抓取（URL 文件）
2. 输出格式支持 html / text / markdown
3. 正文优先提取（优先 article/main，再回退 body）

不适用场景:
1. 页面内容依赖 JavaScript 执行后才出现
2. 目标站点有登录、验证码、反爬、签名校验

示例:
    python3 scripts/fetch_html.py "https://example.com"
    python3 scripts/fetch_html.py "https://example.com" --format text
    python3 scripts/fetch_html.py --url-file scripts/urls.txt --format markdown --out-dir scripts/output
"""

from __future__ import annotations

import argparse
import html
import re
import pathlib
import sys
from html.parser import HTMLParser
from urllib.parse import urlparse
import urllib.error
import urllib.request


DEFAULT_USER_AGENT = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/122.0.0.0 Safari/537.36"
)


def fetch_html(url: str, timeout: int) -> tuple[str, str, int | None, str | None]:
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": DEFAULT_USER_AGENT,
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        },
    )

    with urllib.request.urlopen(request, timeout=timeout) as response:
        final_url = response.geturl()
        status_code = getattr(response, "status", None)
        content_type = response.headers.get("Content-Type")
        raw = response.read()

    encoding = "utf-8"
    if content_type and "charset=" in content_type:
        encoding = content_type.split("charset=", 1)[1].split(";", 1)[0].strip()

    html = raw.decode(encoding, errors="replace")
    return html, final_url, status_code, content_type


BLOCK_TAGS = {
    "p",
    "div",
    "section",
    "article",
    "main",
    "br",
    "li",
    "ul",
    "ol",
    "h1",
    "h2",
    "h3",
    "h4",
    "h5",
    "h6",
}


class TextExtractor(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.skip_depth = 0
        self.parts: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag in {"script", "style", "noscript"}:
            self.skip_depth += 1
            return
        if self.skip_depth == 0 and tag in BLOCK_TAGS:
            self.parts.append("\n")

    def handle_endtag(self, tag: str) -> None:
        if tag in {"script", "style", "noscript"} and self.skip_depth > 0:
            self.skip_depth -= 1
            return
        if self.skip_depth == 0 and tag in BLOCK_TAGS:
            self.parts.append("\n")

    def handle_data(self, data: str) -> None:
        if self.skip_depth == 0:
            cleaned = data.strip()
            if cleaned:
                self.parts.append(cleaned + " ")


def normalize_text(raw: str) -> str:
    text = html.unescape(raw)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def extract_title(html_text: str) -> str:
    match = re.search(r"<title[^>]*>(.*?)</title>", html_text, flags=re.IGNORECASE | re.DOTALL)
    if not match:
        return ""
    return normalize_text(match.group(1))


def find_content_region(html_text: str) -> str:
    patterns = [
        r"<article\b[^>]*>(.*?)</article>",
        r"<main\b[^>]*>(.*?)</main>",
        r"<body\b[^>]*>(.*?)</body>",
    ]
    for pattern in patterns:
        match = re.search(pattern, html_text, flags=re.IGNORECASE | re.DOTALL)
        if match:
            return match.group(1)
    return html_text


def html_to_text(html_text: str) -> str:
    region = find_content_region(html_text)
    parser = TextExtractor()
    parser.feed(region)
    parser.close()
    return normalize_text("".join(parser.parts))


def html_to_markdown(html_text: str) -> str:
    title = extract_title(html_text)
    body_text = html_to_text(html_text)
    if title:
        return f"# {title}\n\n{body_text}".strip()
    return body_text


def build_output_filename(url: str, fmt: str) -> str:
    parsed = urlparse(url)
    host = parsed.netloc or "unknown-host"
    path = parsed.path.strip("/") or "index"
    slug = f"{host}_{path}".replace("/", "_")
    slug = re.sub(r"[^a-zA-Z0-9._-]", "_", slug)
    ext = {"html": "html", "text": "txt", "markdown": "md"}[fmt]
    return f"{slug}.{ext}"


def read_urls(url: str | None, url_file: str | None) -> list[str]:
    urls: list[str] = []
    if url:
        urls.append(url)
    if url_file:
        file_path = pathlib.Path(url_file)
        for line in file_path.read_text(encoding="utf-8").splitlines():
            stripped = line.strip()
            if stripped and not stripped.startswith("#"):
                urls.append(stripped)
    return urls


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="抓取网页并输出 html/text/markdown")
    parser.add_argument(
        "url",
        nargs="?",
        help="要抓取的网页地址，例如 https://example.com",
    )
    parser.add_argument(
        "--url-file",
        help="批量抓取 URL 文件（每行一个 URL，支持 # 注释）",
    )
    parser.add_argument(
        "-o",
        "--output",
        help="单 URL 模式输出文件路径，不传则打印到标准输出",
    )
    parser.add_argument(
        "--out-dir",
        help="批量模式输出目录；单 URL 模式下也可按 URL 自动命名输出",
    )
    parser.add_argument(
        "--format",
        choices=["html", "text", "markdown"],
        default="html",
        help="输出格式，默认 html",
    )
    parser.add_argument(
        "--timeout",
        type=int,
        default=15,
        help="请求超时时间，默认 15 秒",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    urls = read_urls(args.url, args.url_file)
    if not urls:
        print("[ERROR] 请提供 url 或 --url-file", file=sys.stderr)
        return 1
    if len(urls) > 1 and args.output:
        print("[ERROR] 批量模式不支持 --output，请使用 --out-dir", file=sys.stderr)
        return 1
    if len(urls) > 1 and not args.out_dir:
        print("[ERROR] 批量模式请提供 --out-dir", file=sys.stderr)
        return 1

    success = 0
    for url in urls:
        try:
            html_content, final_url, status_code, content_type = fetch_html(url, args.timeout)
        except urllib.error.HTTPError as exc:
            print(f"[ERROR] {url} -> HTTP {exc.code}: {exc.reason}", file=sys.stderr)
            continue
        except urllib.error.URLError as exc:
            print(f"[ERROR] {url} -> 请求失败: {exc.reason}", file=sys.stderr)
            continue
        except Exception as exc:
            print(f"[ERROR] {url} -> 未知异常: {exc}", file=sys.stderr)
            continue

        if args.format == "html":
            rendered = html_content
        elif args.format == "text":
            rendered = html_to_text(html_content)
        else:
            rendered = html_to_markdown(html_content)

        output_path: pathlib.Path | None = None
        if args.output:
            output_path = pathlib.Path(args.output)
        elif args.out_dir:
            output_path = pathlib.Path(args.out_dir) / build_output_filename(final_url, args.format)

        if output_path:
            output_path.parent.mkdir(parents=True, exist_ok=True)
            output_path.write_text(rendered, encoding="utf-8")
            print(f"[OK] {url} -> {output_path.resolve()}")
        else:
            print(rendered)

        print(f"[INFO] URL: {url}", file=sys.stderr)
        print(f"[INFO] 最终地址: {final_url}", file=sys.stderr)
        print(f"[INFO] 状态码: {status_code}", file=sys.stderr)
        print(f"[INFO] Content-Type: {content_type}", file=sys.stderr)
        print(f"[INFO] 内容长度: {len(rendered)} 字符 (format={args.format})", file=sys.stderr)
        success += 1

    return 0 if success == len(urls) else 2


if __name__ == "__main__":
    raise SystemExit(main())
