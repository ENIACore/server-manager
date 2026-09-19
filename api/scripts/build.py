#!/usr/bin/env python3

import subprocess
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_DIR = SCRIPT_DIR.parent
ENV_FILE = SCRIPT_DIR / "env.sh"

command = f"source {ENV_FILE} && mvn clean install"
subprocess.run(command, shell=True, executable="/bin/bash", cwd=PROJECT_DIR, check=True)
