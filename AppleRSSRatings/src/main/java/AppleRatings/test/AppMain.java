package AppleRatings.test;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import com.sun.syndication.feed.synd.SyndEntry;

public class AppMain {
	/**
	 *usage: CommentFetcher
 		-d,--datecolumn <arg>       Zero based index of the date column for sorting
 		-e,--excel                  Use EXCEL format
 		-eto,--emailTo <arg>        Email to. Multiple email addresses should be separated by a space
 		-f,--filename <arg>         File name to save ratings to, will append
 		-h,--help                   Display usage
 		-r,--ratingBoundary <arg>   Send email alert comprising of all star ratings at this number or below (default is 2)
 `		-u,--update                 Append latest updates to file, default is to overwrite with latest 500
	*/
	
	private static CSVFormat mCVSFormat = CSVFormat.DEFAULT;
	private static String[] mEmailToList;
	private static String mFileName;
	private static boolean mUpdateMode=false;
	private static boolean mSendMail=false;
	private static int mColDate=3;
	public static int mAlertRating = 2;
	public static final int ERROR=-1;
	public static final int RETRY=3;
	public static String mStartLink = "https://itunes.apple.com/gb/rss/customerreviews/page=1/id=646362701/sortby=mostrecent/xml?urlDesc=/customerreviews/page=1/id=646362701/sortby=mostrecent/xml";
		
	public static void main(String[] args) {
		AppMain app = new AppMain();
		app.parseOptions(args);
		RatingFetcher ratinghFetcher = new RatingFetcher();
		List<SyndEntry> lowRatingsList = new ArrayList<SyndEntry>();  
		if (mUpdateMode){
			System.out.println("Fetching update");
			lowRatingsList = ratinghFetcher.fetchUpdateAndAppend(mFileName, mCVSFormat, mStartLink, mColDate, RETRY, mAlertRating);
			if(mSendMail){
				ratinghFetcher.sendMail(lowRatingsList, "autotest", mEmailToList, "localhost", 
						"Low Ratings", mAlertRating, "we7phone@gmail.com", "we7rocks");
			}
		}else{
			System.out.println("Fetching all");
			ratinghFetcher.fetchAllAndWrite(mFileName, mCVSFormat, mStartLink, mColDate, RETRY);
		}
	}

	public Options makeOptions(){
		final Options options = new Options();
		options.addOption("f", "filename", true, "File name to save ratings to, will append");
		options.addOption("e", "excel", false, "Use EXCEL format");
		options.addOption("d", "datecolumn", true, "Zero based index of the date column for sorting");
		options.addOption("eto", "emailTo", true, "Email to. Multiple email addresses should be separated by a space");
		options.addOption("r", "ratingBoundary", true, "Send email alert comprising of all star ratings at this number or below (default is 2)");
		options.addOption("u", "update", false, "Append latest updates to file, default is to overwrite with latest 500");
		options.addOption("h", "help", false, "Display usage");
		return options;
	}
	
	public void parseOptions(String[] args){
		HelpFormatter formatter = new HelpFormatter();
		final CommandLineParser cmdLineGnuParser = new GnuParser();
		final Options options = makeOptions(); 
		CommandLine commandLine = null;
		try {
			commandLine = cmdLineGnuParser.parse(options, args);
		} catch (Exception e) {
			e.printStackTrace();
			formatter.printHelp( "CommentFetcher", options);
			System.exit(ERROR);
		}
		if (commandLine.getOptions().length == 0){
			formatter.printHelp( "CommentFetcher", options);
			System.out.println("0 args supplied");
			System.exit(ERROR);
		}
		//=======================================================================================
			
		if (commandLine.hasOption('f') && commandLine.getOptionValue('f') != null){
			mFileName = commandLine.getOptionValue('f');  
		}else {
			System.out.println("You must supply a file name");
			formatter.printHelp("CommentFetcher", options);
			System.exit(ERROR);
		}
		if (commandLine.hasOption('d') && commandLine.getOptionValue('d') != null  && isNumeric(commandLine.getOptionValue('d'))){
			mColDate = Integer.parseInt(commandLine.getOptionValue('d'));  
		}else {
			System.out.println("You must supply a value for the date column");
			formatter.printHelp("CommentFetcher", options);
			System.exit(ERROR);
		}
		if (commandLine.hasOption("eto")){
			mSendMail=true;
			mEmailToList = commandLine.getOptionValue("eto").split(","); 
		}
		if (commandLine.hasOption('u')){
			if(commandLine.getOptionValue('r') != null && isNumeric(commandLine.getOptionValue('r'))){
				mAlertRating =  Integer.parseInt(commandLine.getOptionValue('r')); 
			}else{
				formatter.printHelp( "CommentFetcher", options);
				System.exit(ERROR);
			}
		}
		if (commandLine.hasOption('e')){
			AppMain.mCVSFormat = CSVFormat.EXCEL;
		}
		if (commandLine.hasOption('u')){
			mUpdateMode = true;
		}
		if (commandLine.hasOption('h')){
			formatter.printHelp( "CommentFetcher", options);
			System.exit(1);
		}
	}
	//=======================================================================================
	
	public static boolean isNumeric(String str){  
	  try {  
	    Integer.parseInt(str);  
	  }  
	  catch(NumberFormatException nfe){  
	    return false;  
	  }  
	  return true;  
	}
}
