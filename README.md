# facebook-data-image-exif

CURRENTLY BROKEN.....

Originally created as part of https://addshore.com/2016/09/add-exif-data-back-to-facebook-images/
Thrown into git & github in 2018 and mashed together with docker to make it easier....

CURRENTLY BROKEN.....

## TODOs

The facebook export format has changed:
 - index.html files are no longer inside the album directories, instead the photos directory has all of the html files, each of which has the album ID as the name.
 - The html files no longer seem to link to the images that have been downloaded, but seem to link to the facebook CDN via HTTPs src links....
   - Because of this the converting of the src to the img location in the photos dir needs to change slightly...
   - Need to parse links like this: https://scontent-lhr3-1.xx.fbcdn.net/v/t31.0-0/p600x600/929219241_1014210984210912680086_200094218092181680132_o.jpg?_nc_ad=z-m&_nc_cid=0&oh=u21jr90i2196ffed0b257c192eee67&oe=5BHHDAA4

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