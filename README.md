# facebook-data-image-exif

Originally created as part of https://addshore.com/2016/09/add-exif-data-back-to-facebook-images/
Thrown into git & github in 2018 and mashed together with docker to make it easier....
Also altered to work with the new facebook export format (not too thoroughly tested)....

This script will go through all photos in all albums and parse data from the relevant html file adding exif data where possible.
It will rename the original photos using a _original suffix and the exif data will be added to copies in their old location.

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