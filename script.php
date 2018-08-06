<?php

// Licence GPL 2+ https://www.gnu.org/licenses/gpl-2.0.en.html
// @TODO: Replace default UTC
date_default_timezone_set('UTC');

/**
 * Read exported Facebook JSON for photos, apply exif data to originals
 */
class FacebookEXIF
{

	function __construct()
	{
		// Directory of the extracted facebook dump photos
		// Example: 'C:/Users/username/Downloads/facebook-username/photos'
		// Or mac: /Users/myname/Downloads/photos_and_videos
		$this->directory = getenv( 'photosdirectory' );
		// Location of the exiftool script
		$this->tool = getenv( 'exiftool' );

		echo "Starting\n";
		echo "Using exiftool location: '" . $this->tool . "'\n";
		echo "Using input location: '" . $this->directory . "'\n";

		$this->getPhotos();

		echo "Done!\n";

	}

	private function getPhotos()
	{
		$jsonAlbums = glob( $this->directory . '/album/*.json' );
		if(empty($jsonAlbums)) {
			throw new Exception("Could not find any JSON files in album directory");
		}
		$count = count( $jsonAlbums );
		echo "Got " . $count . " albums from the input directory.\n";
		foreach ($jsonAlbums as $i => $album) {
			$json = json_decode(file_get_contents($album), true);
			if(!$json || empty($json['photos'])) {
				continue;
			}
			echo "\n=== Album $i/$count {$json['name']} ===\n\n";
			$this->processPhotos($json['photos']);
		}
	}

	private function processPhotos($photos)
	{
		if(empty($photos)) {
			throw new Exception("No photo information");
		}
		// This array holds the EXIF name in the key and FB meta in the value
		$possibleExif = array(
			'Make' => "camera_make",
			'Model' => "camera_model",
			'DateTimeOriginal' => "taken_timestamp",
			'ModifyDate' => "modified_timestamp",
			'Exposure' => "exposure",
			'FocalLength' => "focal_length",
			'FNumber' => "f_stop",
			'ISO' => "iso_speed",
			'GPSLatitude' => "latitude",
			'GPSLongitude' => "longitude",
			'GPSLatitudeRef' => "latitude",
			'GPSLongitudeRef' => "longitude",
			// '' => "orientation", this will cause odd rotations (as facebook has already rotated the image)
		);

		// Loop through all photos, grab data and modify file
		foreach ($photos as $photoData) {
			$metaData = $photoData['media_metadata']['photo_metadata'];
			$toChange = array();
			if($metaData['taken_timestamp']) {
				// Keep timestamp as is
			} else if ($metaData['modified_timestamp']) {
				// It's missing, replace with modified
				$metaData['taken_timestamp'] = $metaData['modified_timestamp'];
			} else {
				$metaData['taken_timestamp'] = $photoData['creation_timestamp'];
			}
			$metaData['taken_timestamp'] = date('Y:m:d G:i:s', $metaData['taken_timestamp']);
			if($metaData['modified_timestamp']) {
				$metaData['modified_timestamp'] = date('Y:m:d G:i:s', $metaData['modified_timestamp']);
			} else {
				$metaData['modified_timestamp'] = date('Y:m:d G:i:s');
			}
			if($metaData['f_stop']) {
				$parts = explode('/', $metaData['f_stop']);
				if(count($parts) > 1) {
					$metaData['f_stop'] = $parts[0] / $parts[1];
				}
			}
			foreach ($possibleExif as $exifValue => $metaValue) {
				if(array_key_exists($metaValue, $metaData)) {
					$toChange[] = '"-EXIF:'.$exifValue.'=' . $metaData[$metaValue] . '"';
				}
			}
			// Default a 0 altitude if lat/lon given
			if(array_key_exists('latitude', $metaData)) {
				$toChange[] = '"-EXIF:GPSAltitude=' . '0' . '"';
			}
			$imgLocation = $this->directory . str_replace('photos_and_videos', '', $photoData['uri']);
			echo "Running for file $imgLocation\n";
			exec( $this->tool . ' ' . implode( ' ', $toChange ) . ' -overwrite_original ' . $imgLocation );
		}

	}

}

new FacebookEXIF;
