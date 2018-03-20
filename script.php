<?php

// Licence GPL 2+ https://www.gnu.org/licenses/gpl-2.0.en.html

// Directory of the extracted facebook dump photos
// Example: 'C:/Users/username/Downloads/facebook-username/photos'
$directory = getenv( 'photosdirectory' );
// Location of the exiftool script
$tool = getenv( 'exiftool' );

echo "Starting\n";
echo "Using exiftool location: '" . $tool . "'\n";
echo "Using input location: '" . $directory . "'\n";

$albums = glob( $directory . '/*', GLOB_ONLYDIR );

echo "Got " . count( $albums ) . " albums from the input directory.\n";

foreach ( $albums as $albumLocation ) {
	echo "Running for album $albumLocation\n";
	$indexFile = $albumLocation . '/index.htm';
	$dom = DOMDocument::loadHTMLFile( $indexFile );
	$finder = new DomXPath( $dom );
	$blockNodes = $finder->query( "//*[contains(concat(' ', @class, ' '), ' block ')]" );
	foreach ( $blockNodes as $blockNode ) {
		$imageNode = $blockNode->firstChild;
		$imgSrc = $imageNode->getAttribute( 'src' );
		$imgSrcParts = explode( '/', $imgSrc );
		$imgSrc = array_pop( $imgSrcParts );
		$imgLocation = $albumLocation . '/' . $imgSrc;

		echo "Running for file $imgLocation\n";

		$details = array();
		$metaDiv = $blockNode->lastChild;
		$details['textContent'] = $metaDiv->firstChild->textContent;
		$metaTable = $metaDiv->childNodes->item( 1 );
		foreach ( $metaTable->childNodes as $rowNode ) {
			$details[$rowNode->firstChild->textContent] = $rowNode->lastChild->textContent;
		}

		$toChange = '';

		$toChange[] = '"-EXIF:ModifyDate=' . date_format( new DateTime(), 'Y:m:d G:i:s' ) . '"';

		if ( array_key_exists( 'Taken', $details ) ) {
			$toChange[] = '"-EXIF:DateTimeOriginal=' .
				date_format( new DateTime( "@" . $details['Taken'] ), 'Y:m:d G:i:s' ) .
				'"';
		} else {
			continue;
		}
		if ( array_key_exists( 'Camera Make', $details ) ) {
			$toChange[] = '"-EXIF:Make=' . $details['Camera Make'] . '"';
		}
		if ( array_key_exists( 'Camera Model', $details ) ) {
			$toChange[] = '"-EXIF:Model=' . $details['Camera Model'] . '"';
		}
		// Doing this will cause odd rotations.... (as facebook has already rotated the image)...
		//      if ( array_key_exists( 'Orientation', $details ) ) {
		//          $toChange[] = '"-EXIF:Orientation=' . $details['Orientation'] . '"';
		//      }
		if ( array_key_exists( 'Latitude', $details ) && array_key_exists( 'Longitude', $details ) ) {
			$toChange[] = '"-EXIF:GPSLatitude=' . $details['Latitude'] . '"';
			$toChange[] = '"-EXIF:GPSLongitude=' . $details['Longitude'] . '"';
			// Tool will look at the sign used for NSEW!
			$toChange[] = '"-EXIF:GPSLatitudeRef=' . $details['Latitude'] . '"';
			$toChange[] = '"-EXIF:GPSLongitudeRef=' . $details['Longitude'] . '"';
			$toChange[] = '"-EXIF:GPSAltitude=' . '0' . '"';
		}

		exec( $tool . ' ' . implode( ' ', $toChange ) . ' ' . $imgLocation );
	}
}

echo "Done!\n";