#!/usr/bin/env python3
import argparse
import json
from pathlib import Path


def load_properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        result[key.strip()] = value.strip().replace("\\:", ":")
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--gradle-properties", required=True)
    parser.add_argument("--apk-url", required=True)
    parser.add_argument("--page-url", required=True)
    parser.add_argument("--sha256", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--file-name", default="app.apk")
    parser.add_argument("--abi-code", type=int, default=9)
    parser.add_argument("--changelog-file")
    # Delta patch support: pass a JSON array of delta entries.
    # Each entry must contain at minimum: fromVersionCode (int), url (str), sha256 (str).
    # Example: '[{"fromVersionCode":66661890,"url":"https://...","sha256":"ABC...","size":4500000}]'
    parser.add_argument(
        "--delta-json",
        default=None,
        help="JSON array of delta patch entries to embed in the manifest",
    )
    args = parser.parse_args()

    props = load_properties(Path(args.gradle_properties))
    base_version_name = props["APP_VERSION_NAME"]
    base_version_code = int(props["APP_VERSION_CODE"])
    patch_version = int(props.get("FLUFFY_PATCH_VERSION", "0"))

    version_name = f"{base_version_name}.{patch_version}" if patch_version > 0 else base_version_name
    version_code = (base_version_code * 1000 + patch_version) * 10 + args.abi_code

    changelog = ""
    if args.changelog_file:
        changelog = Path(args.changelog_file).read_text(encoding="utf-8").strip()

    payload: dict = {
        "version": version_name,
        "versionCode": version_code,
        "apkUrl": args.apk_url,
        "pageUrl": args.page_url,
        "sha256": args.sha256.upper(),
        "fileName": args.file_name,
        "changelog": changelog,
    }

    if args.delta_json:
        try:
            deltas = json.loads(args.delta_json)
            if isinstance(deltas, list) and deltas:
                payload["deltas"] = deltas
        except json.JSONDecodeError as exc:
            raise SystemExit(f"--delta-json is not valid JSON: {exc}") from exc

    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
