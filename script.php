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
	$albumName = str_replace( $directory . DIRECTORY_SEPARATOR, '', $albumLocation );
	$htmlLocation = $directory . DIRECTORY_SEPARATOR .  $albumName . '.html';
	echo "Running for album $albumName\n";
	$dom = DOMDocument::loadHTMLFile( $htmlLocation );
	$finder = new DomXPath( $dom );
	$blockNodes = $finder->query( "//*[contains(concat(' ', @class, ' '), ' block ')]" );
	foreach ( $blockNodes as $blockNode ) {
		$parsedUrl = parse_url( $blockNode->firstChild->getAttribute( 'src' ) );

		// Break up the parsed URL a bit
		$srcPath = $parsedUrl['path'];
		$srcPathParts = explode( '/', $srcPath );
		$srcFileName = array_pop( $srcPathParts );
		$srcFileNameParts = explode( '_', $srcFileName );

		// Set the file ID and type
		$imgId = $srcFileNameParts[1];
		// I'm pretty sure this is always actually jpg...
		$imgType = explode( '.', array_pop( $srcFileNameParts ) )[1];

		// Come up with the local file location
		$imgLocation = $albumLocation . DIRECTORY_SEPARATOR . $imgId . '.' . $imgType;

		echo "Running for file $imgLocation\n";

		$details = array();
		$metaDiv = $blockNode->lastChild;
		$details['textContent'] = $metaDiv->firstChild->textContent;
		$metaTable = $metaDiv->childNodes->item( 1 );
		foreach ( $metaTable->childNodes as $rowNode ) {
			$details[$rowNode->firstChild->textContent] = $rowNode->lastChild->textContent;
		}

		$toChange = [];

		// TODO eww hardcoded UTC....
		$toChange[] = '"-EXIF:ModifyDate=' . date_format( new DateTime( 'now', new DateTimeZone( 'UTC' ) ), 'Y:m:d G:i:s' ) . '"';

		if ( array_key_exists( 'Taken', $details ) ) {
			$toChange[] = '"-EXIF:DateTimeOriginal=' .
				date_format( new DateTime( "@" . $details['Taken'], new DateTimeZone( 'UTC' ) ), 'Y:m:d G:i:s' ) .
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