# facebook-data-image-exif (legacy PHP version)

These is now also a Java version of this code with a simple UI.
(See the root README of this repo and the java directory).

Originally created in 2016 for a blog post on how to [Add Exif data back to Facebook images](https://addshore.com/2016/09/add-exif-data-back-to-facebook-images/)
Thrown into git & github in 2018 and mashed together with Docker to make it easier to run with dependencies.
Modified to use the newly provided JSON format mandated by GDPR in 2018. [PR](https://github.com/addshore/facebook-data-image-exif/pull/1)

This script will go through all photos in all albums and parse data from the relevant json file adding exif data where possible.

No backup is created as part of this script, please make your own. You'll need to request a JSON backup of your photos from Facebook.

## Usage (with Docker & linux containers)

 - Install [Docker](https://www.docker.com/community-edition#/download)
 - Clone this repo (or download this code)
 - Open a terminal in the repo directory
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
