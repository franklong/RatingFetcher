RatingFetcher
=============

 **usage: appleRatingFetcher**
 
 		-d,--datecolumn <arg>       Zero based index of the date column for sorting
 		
 		-e,--excel                  Use EXCEL format
 		
 		-eto,--emailTo <arg>        Email to. Multiple email addresses should be separated by a comma
 		
 		-f,--filename <arg>         File name to save ratings to, will append
 		
 		-h,--help                   Display usage
 		
 		-r,--ratingBoundary <arg>   Send email alert comprising of all star ratings at this number or below (default is 2)
 		
 		-u,--update                 Append latest updates to file, default is to overwrite with latest 500
	
		 
		 //Does not use the DiskFeedInfoCache Caching functionality built into ROME, this doesn't always work
