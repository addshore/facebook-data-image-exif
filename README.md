# facebook-data-image-exif

Originally created as part of https://addshore.com/2016/09/add-exif-data-back-to-facebook-images/
Thrown into git & github in 2018 and mashed together with docker to make it easier....

## Usage (with Docker & linux containers)

 - Install docker
 - Clone this repo (or download this code)
 - Build and tag the image - `docker build -t facebook-data-image-exif .`
 - Run the script with the correct things passed in: `docker run --rm -it -v //path/to/facebook/export/photos/directory://input facebook-data-image-exif`

## Usage (without Docker)

 - Install PHP - https://secure.php.net/manual/en/install.php
 - Download exiftool - http://www.sno.phy.queensu.ca/~phil/exiftool/
 - Clone the repo (or copy the script (script.php))
 - Setup the 2 environment variables used by the script (or modify the script to add the correct paths)
   - photosdirectory
   - exiftool
 - Run the script using php `php script.php`