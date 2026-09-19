#!/usr/bin/env python3

import subprocess
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
ENV_FILE = SCRIPT_DIR / "env.sh"

command = (
    f"source {ENV_FILE} && "
    "docker run --name server-manager-db "
    "-e POSTGRES_DB=$POSTGRES_DB "
    "-e POSTGRES_USER=$POSTGRES_USER "
    "-e POSTGRES_PASSWORD=$POSTGRES_PASSWORD "
    "-p 5432:5432 "
    "-v server-manager-db-data:/var/lib/postgresql "
    "-d postgres:latest"
)
subprocess.run(command, shell=True, executable="/bin/bash", check=True)
