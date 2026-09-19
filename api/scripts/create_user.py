#!/usr/bin/env python3

import getpass
import subprocess
from pathlib import Path

import bcrypt

SCRIPT_DIR = Path(__file__).resolve().parent
ENV_FILE = SCRIPT_DIR / "env.sh"


def read_env():
    result = subprocess.run(
        f"source {ENV_FILE} && printf '%s\\n' \"$POSTGRES_DB\" \"$POSTGRES_USER\" \"$POSTGRES_PASSWORD\"",
        shell=True,
        executable="/bin/bash",
        capture_output=True,
        text=True,
        check=True,
    )
    postgres_db, postgres_user, postgres_password = result.stdout.splitlines()
    return postgres_db, postgres_user, postgres_password


def main():
    postgres_db, postgres_user, postgres_password = read_env()

    username = input("Username: ").strip()
    password = getpass.getpass("Password: ")

    password_hash = bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt(rounds=10)).decode("utf-8")

    sql = (
        "INSERT INTO users (username, password, role, enabled) "
        "VALUES (:'username', :'password_hash', 'USER', true);"
    )

    subprocess.run(
        [
            "docker", "exec", "-i",
            "-e", f"PGPASSWORD={postgres_password}",
            "server-manager-db",
            "psql", "-U", postgres_user, "-d", postgres_db,
            "-v", f"username={username}",
            "-v", f"password_hash={password_hash}",
        ],
        input=sql,
        text=True,
        check=True,
    )

    print(f"Created user '{username}'.")


if __name__ == "__main__":
    main()
