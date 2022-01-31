#### 0.12 (21 January 2021)

* Update expected directory name from 'photos_and_videos' to 'posts' ([Thanks loganrosen](https://github.com/addshore/facebook-data-image-exif/pull/22))

#### 0.11-alpha1 (development)

* Built with JFK 11 instead of JDK 8

#### 0.10 (5 April 2020)

* Detect exiftool(-k).exe use on Windows and error
* Make the duplication of all images (backup) optional
* Fallback to the "creation_timestamp" of the FB upload rather than using image data for very old uploads

#### 0.9 (12 March 2019)

* Detect missing files from the facebook download and error
* Detect unwritable files in the facebook data and error
* Exiftool uses a pool and reuses the same process where possible (speed improvement)
* Output in list instead of large text area (better performance for large data downloads)
* Application will actually stop if closed once task has already started

#### 0.8 (1 March 2019)

* Error popup now shown if one or more fields are missing
* Added browse buttons for the 2 input fields
* Even more debug output (including the data extracted for each image)

#### 0.7 (27 Feb 2019)

* Catch and helpfully show more errors
* Show summary if images processed at the bottom of output

#### 0.6 (27 Feb 2019)

* Even more debug output while task is running
* Windows users now need only specify the directory of exiftool.exe
* Bugfix, script would fail is creation_timestamp was not found in exif data
* Bugfix, issue when image data is not in media_metadata/photo_metadata but in the root instead

#### 0.5 (23 Feb 2019)

* Output exiftool version
* Only require the path to the extracted facebook download now
* Error if exiftool is not called exiftool.exe on Windows

#### 0.4 (23 Feb 2019)

* Cleaned up the UI further
* Made dry run mode a button rather than a check box
* Use File.separator to get correct separator for differing OSs

#### 0.3 (22 Feb 2019)

* Cleaned up the main UI a little
* Added note and output when used with a HTML data download (not supported yet)
* No longer bundeling exiftool.exe in a windows build (things were going wrong)
* Detecting exiftool in PATH, but allowing the user to override

#### 0.2 (20 Feb 2019)

* Added Version, OS and options to task output
* Added extra optional debug output
* Added a dry run mode

#### 0.1 (10 Feb 2019)

* Initial release