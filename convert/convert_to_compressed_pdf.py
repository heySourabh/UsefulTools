'''
Desc:
    Program to convert to pdf and generate good quality compressed pdf.
    Usage: python3 convert_to_compressed_pdf.py file[.pdf|.png|.jpeg|.jpg|.svg]
    If a image file is provided then the program converts the image to pdf.
    The software 'inkscape' and 'gs' must be available on the $PATH.

Author: 
    Sourabh Bhat <heySourabh@gmail.com>
'''
import sys
import subprocess
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


def compress_pdf(pdf_path, gs_out_path):
    check_exists_and_exit(gs_out_path)
    # Call gs to convert to pdf
    subprocess.call(["gs",
                     "-sDEVICE=pdfwrite",
                     "-dCompatibilityLevel=1.5",
                     "-dPDFSETTINGS=/ebook",
                     "-dNOPAUSE",
                     "-dBATCH",
                     "-dQUIET",
                     f"-sOutputFile={gs_out_path}",
                     f"{pdf_path}"])


def convert_image(image_path):
    extn = image_path.split(".")[-1]
    pdf_path = image_path.replace(f".{extn}", ".pdf")
    check_exists_and_exit(pdf_path)
    # Call inkscape to convert to pdf
    subprocess.call(["inkscape",
                     f"--export-filename={pdf_path}",
                     image_path])
    gs_out_path = pdf_path.replace(".pdf", "_compressed.pdf")
    compress_pdf(pdf_path, gs_out_path)
    os.remove(pdf_path)
    os.rename(gs_out_path, pdf_path)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Missing single argument: file path of image or pdf.")
        exit(1)

    file_path = sys.argv[1]
    image_file_extns = ["jpg", "jpeg", "png", "svg"]
    extn = file_path.split(".")[-1].strip().lower()
    if extn in image_file_extns:
        convert_image(file_path)

    if extn == "pdf":
        gs_out_path = file_path.replace(".pdf", "_compressed.pdf")
        compress_pdf(file_path, gs_out_path)
