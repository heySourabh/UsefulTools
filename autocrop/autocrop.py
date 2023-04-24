#!/usr/bin/python3
'''
author: Sourabh Bhat <https://spbhat.in>
'''

import subprocess
import sys
import os


def notify_error(message):
    print(message)
    subprocess.call([
        "notify-send",
        "Error",
        message,
    ])


def check_exists_and_exit(file_path):
    if os.path.exists(file_path):
        notify_error(f"File: '{file_path}' arleady exists.")
        exit(1)


def autocrop(filepath: str):

    extn = filepath.split(".")[-1]
    cropped_filepath = filepath.replace(f".{extn}", f"_cropped.{extn}")
    check_exists_and_exit(cropped_filepath)

    subprocess.call(["convert",
                     "-strip",
                    "-trim", f"{filepath}",
                     f"{cropped_filepath}"])


if __name__ == "__main__":
    if len(sys.argv) != 2:
        exit(1)

    filepath = sys.argv[1].strip()
    if filepath.split(".")[-1] in ["png", "jpg", "jpeg"]:
        autocrop(filepath)
    else:
        exit(1)
