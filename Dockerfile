FROM php:5-cli

WORKDIR /

# Get ExifTool and put it in /exiftool
# TODO remove wget gzip and tar?
RUN  apt-get update \
  && apt-get install -y wget gzip tar\
  && wget https://www.sno.phy.queensu.ca/~phil/exiftool/Image-ExifTool-10.86.tar.gz\
  && gzip -dc Image-ExifTool-10.86.tar.gz | tar -xf -\
  && rm Image-ExifTool-10.86.tar.gz\
  && rm -rf /var/lib/apt/lists/*\
  && mkdir /exiftool\
  && mv Image-ExifTool-10.86/* /exiftool/\
  && rmdir Image-ExifTool-10.86

ENV exiftool /exiftool/exiftool
ENV photosdirectory /input

COPY script.php /script.php
RUN mkdir /input

CMD [ "php", "/script.php" ]