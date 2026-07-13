#!/usr/bin/env python3
"""Generates release metadata from the mod descriptor:

- build/release/dependencies.json  - machine-readable {modId: versionRange} of required
  dependencies; the in-game updater downloads this to verify a server has everything the
  new version needs before auto-updating.
- build/release/release_notes.md   - human-readable release body with the dependency table.
"""
import json
import pathlib
import re
import tomllib

ROOT = pathlib.Path(__file__).resolve().parent.parent
MODS_TOML = ROOT / "src/main/resources/META-INF/neoforge.mods.toml"
GRADLE_PROPERTIES = ROOT / "gradle.properties"
OUT_DIR = ROOT / "build/release"

MODRINTH_LINKS = {
    "create": "https://modrinth.com/mod/create",
    "sophisticatedcore": "https://modrinth.com/mod/sophisticated-core",
    "sophisticatedbackpacks": "https://modrinth.com/mod/sophisticated-backpacks",
    "create_mobile_packages": "https://modrinth.com/mod/create-mobile-packages",
}


def main() -> None:
    version = re.search(r"^mod_version=(.+)$", GRADLE_PROPERTIES.read_text(), re.M).group(1).strip()

    # the descriptor templates ${version}; substitute so tomllib can parse it
    toml_text = MODS_TOML.read_text().replace("${version}", version)
    descriptor = tomllib.loads(toml_text)

    dependencies = {}
    for dep in descriptor.get("dependencies", {}).get("backpack_logistics", []):
        if dep.get("type", "required") == "required":
            dependencies[dep["modId"]] = dep["range"]

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    (OUT_DIR / "dependencies.json").write_text(json.dumps(dependencies, indent=2) + "\n")

    lines = [
        f"## Create: Backpack Logistics {version}",
        "",
        "Backpack upgrade modules bridging Create logistics networks, Create Mobile Packages",
        "and Sophisticated Backpacks: Auto-Unpacker, Stock Caller and Sender (basic + advanced).",
        "",
        "### Required dependencies",
        "",
        "| Mod | Version range |",
        "| --- | --- |",
    ]
    for mod_id, version_range in dependencies.items():
        link = MODRINTH_LINKS.get(mod_id)
        name = f"[{mod_id}]({link})" if link else mod_id
        lines.append(f"| {name} | `{version_range}` |")
    lines += [
        "",
        "### Installation",
        "",
        "Drop the jar into the `mods/` folder on the **server and every client**.",
        "Servers with auto-update enabled (default) pick new releases up automatically on the",
        "next restart, and hand the new version to outdated clients when they join",
        "(both behaviours can be disabled in `config/backpack_logistics-common.toml`).",
        "",
        "`dependencies.json` is consumed by the in-game updater - servers only auto-update",
        "when every dependency listed there is installed in a matching version.",
    ]
    (OUT_DIR / "release_notes.md").write_text("\n".join(lines) + "\n")
    print(f"wrote {OUT_DIR / 'dependencies.json'} and {OUT_DIR / 'release_notes.md'}")


if __name__ == "__main__":
    main()
