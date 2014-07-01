package AppleRatings.test;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jdom.Element;
import org.rendersnake.HtmlAttributes;
import org.rendersnake.HtmlCanvas;
import org.rometools.fetcher.FeedFetcher;
import org.rometools.fetcher.FetcherEvent;
import org.rometools.fetcher.FetcherListener;
import org.rometools.fetcher.impl.DiskFeedInfoCache;
import org.rometools.fetcher.impl.HttpURLFeedFetcher;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndLink;

public class RatingFetcher{
	/**
	 *usage: CommentFetcher
		 -e,--excel            Use EXCEL format
		 -f,--filename <arg>   File name to save comments to, will append
		 -d,--datecolumn       Sort on multiple columns (takes a comma delimited string of zero based numbers e.g. 2,1,3)
		 -u,--update           Append latest updates to file, default is to overwrite with latest 500
		 -h,--help             Display usage
		 
		 //Does not use the DiskFeedInfoCache Caching functionality built into ROME, this doesn't always work
	*/
	private CSVPrinter mPrinter = null;
	public static final int ERROR=-1; 
	public static String mStartLink = "https://itunes.apple.com/gb/rss/customerreviews/page=1/id=646362701/sortby=mostrecent/xml?urlDesc=/customerreviews/page=1/id=646362701/sortby=mostrecent/xml";
		
	public void fetchAllAndWrite(final String pFilename, CSVFormat pFormat, final String pStartURL, final int pDateColumnIndex, final int pRetry){
		List<SyndEntry> allEnteries = fetchAll(pStartURL, pRetry);
		removeNonRatingsSynd(allEnteries);
		writeCommentData(allEnteries, pFilename, pFormat, false);
		List<CSVRecord> allRecords = readAllRecordsFromFile(pFilename, pFormat);
		sortCommentsListByDate(allRecords, pDateColumnIndex);
		writeRecordData(allRecords, pFilename, pFormat);
	}
	
	@SuppressWarnings("unchecked")
	public List<SyndEntry> getLowRatings(final List<SyndEntry> pUpdateList, final int pRating){
		List<SyndEntry> lowRatings = new ArrayList<SyndEntry>();
		for (SyndEntry entry : pUpdateList){
			List<Element> foreignMarkups = (List<Element>) entry.getForeignMarkup();	
			 for (Element foreignMarkup : foreignMarkups){
					 if (foreignMarkup.getName().equals("rating")) {
						 if (Integer.parseInt(foreignMarkup.getValue()) <= pRating){
							 lowRatings.add(entry);
						 }
				 	 }
			 }
		}
		return lowRatings;
	}
	
	public List<SyndEntry> fetchUpdateAndAppend(final String pFilename, CSVFormat pFormat, final String pStartURL, int pDateColumnIndex, final int pRetry, final int pRatingAlert){
		Date latestDate = getLatestDateFromFile(pFilename, pFormat, pDateColumnIndex);
		List<SyndEntry> latestEnteries = fetchLatest(pStartURL, latestDate, pRetry);
		List<SyndEntry> lowEnteries = getLowRatings(latestEnteries, pRatingAlert);
		writeCommentData(latestEnteries, pFilename, pFormat, true);
		List<CSVRecord> allRecords = readAllRecordsFromFile(pFilename, pFormat);
		sortCommentsListByDate(allRecords, pDateColumnIndex);
		writeRecordData(allRecords, pFilename, pFormat);
		return lowEnteries;
	}
		
	public String getLinkText(final String pRelName, final SyndFeed pFeed){
		String relName="";
		@SuppressWarnings("unchecked")
		List<SyndLink> linkList = pFeed.getLinks();
		for (SyndLink link : linkList){
			if ((pRelName).equals(link.getRel())){
				relName = link.getHref();
			}
		}
		return relName;
	}
	
	@SuppressWarnings("unchecked")
	public void removeNonRatingsSynd(final List<SyndEntry> pListSynd){
		for (Iterator<SyndEntry> i = pListSynd.iterator(); i.hasNext();) {
			boolean isRating=false;
			SyndEntry entry = i.next();
			List<Element> foreignMarkups = (List<Element>) entry.getForeignMarkup();
		    for (Element foreignMarkup : foreignMarkups){
				 if (foreignMarkup.getName().equals("rating")) {
					 isRating = true;
			 	 }
			 }
		     if (!isRating) {
		        i.remove();
		    }
		}
	}
		
	@SuppressWarnings("unchecked")
	public void writeCommentData(final List<SyndEntry> pSyndEntryList, final String pFileName, final CSVFormat pFormat, final boolean pAppend){
		try {
			mPrinter = new CSVPrinter(new PrintWriter(new FileWriter(pFileName, pAppend)), pFormat);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(ERROR);
		}
		
		System.out.println(String.format("Printing %d record(s) to file [%s]", pSyndEntryList.size(), pFileName));
		for (SyndEntry entry :pSyndEntryList){
			 List<String> record = new ArrayList<String>();
			 record.add(entry.getUpdatedDate().toString());
			 record.add(entry.getUri());
			 record.add(entry.getAuthor());
			 record.add(entry.getTitle());
			 SyndContent content = (SyndContent) entry.getContents().get(0);
			 record.add(content.getValue().replaceAll("\\n", ""));
			 List<Element> foreignMarkups = (List<Element>) entry.getForeignMarkup();
			 for (Element foreignMarkup : foreignMarkups){
				 if (foreignMarkup.getName().equals("rating")) {
					 record.add(foreignMarkup.getValue());
				 }
			}
			 try {
				mPrinter.printRecord(record);
			 } catch (IOException e1) {
				e1.printStackTrace();
				System.exit(ERROR);
			 }
		}
		try {
			mPrinter.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(ERROR);
		}
	}
	
	public List<SyndEntry> fetchLatest(final String pURL, final Date pLastDate, final int pRetry){
		List<SyndEntry> latestRecords = new ArrayList<SyndEntry>();
		List<SyndEntry> allRecords = fetchAll(pURL, pRetry);
		removeNonRatingsSynd(allRecords);
		for (SyndEntry entry : allRecords){
			if (entry.getUpdatedDate().after(pLastDate)){
				latestRecords.add(entry);
			}
		}
		System.out.println(String.format("Found %d new record(s) with a date more recent than [%s] ", latestRecords.size(), pLastDate.toString()));
		if (latestRecords.size() == 0){
			System.exit(1);	
		}
		return latestRecords;
	}
			
	@SuppressWarnings("unchecked")
	public List<SyndEntry> fetchAll(final String pURL, final int pRetry){
		  FeedFetcher feedFetcher = new HttpURLFeedFetcher();
		  feedFetcher.addFetcherEventListener(new FetcherListener(){
			public void fetcherEvent(FetcherEvent event) {
				if(FetcherEvent.EVENT_TYPE_FEED_RETRIEVED.equals(event.getEventType())){
					System.out.println("- OK " + event.getUrlString());
				}
			}
		  });
		  List<SyndEntry> allEnteries = new ArrayList<SyndEntry>();
		  String next = pURL;
		  String self = pURL;
		  String last = null;
		  URL fetchURL = null;
		  SyndFeed feed = null;
		  int pageCount=10;
		  int page=0;
		  while (!self.equals(last) || page < pageCount) {
				System.out.print(String.format("Fetching ratings - page %02d ", ++page));
				try {
				  fetchURL = new URL(next);
				} catch (MalformedURLException e) {}
				try {
					feed = feedFetcher.retrieveFeed(fetchURL);
					last = getLinkText("last", feed).split("\\?")[0]; //remove descriptor for comparison
					next = getLinkText("next", feed);
					self = getLinkText("self", feed).split("\\?")[0]; //remove descriptor for comparison
					allEnteries.addAll(feed.getEntries());
				 } catch (Exception ex) {
						ex.printStackTrace();
						System.exit(-1);
				 }
		   }		
		   return allEnteries;
	}
	
	public List<CSVRecord> readAllRecordsFromFile(final String pFilename, final CSVFormat pFormat){
		List<CSVRecord> sortableRecords = new ArrayList<CSVRecord>();
		Reader in = null;
		try {
			in = new FileReader(pFilename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(ERROR);
		}
		Iterable<CSVRecord> records = null;
		try {
			records = pFormat.parse(in);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(ERROR);
		}
	
		for (CSVRecord record : records) {
			sortableRecords.add(record);
		}
		if(in != null){
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(ERROR);
			}	
		}
		System.out.println(String.format("Read %d record(s) in file [%s] for sorting", sortableRecords.size(), pFilename));
		return sortableRecords;
	}
	
	public Date getLatestDateFromFile(final String pFilename, final CSVFormat pFormat, final int pDateColumnIndex){
		Date date = null;
		Date latest = new Date();
		Reader in = null;
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -100);
		latest = cal.getTime();

		try {
			in = new FileReader(pFilename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(ERROR);
		}
		Iterable<CSVRecord> records = null;
		try {
			records = pFormat.parse(in);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(ERROR);
		}
	    DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss zzz yyyy");
		for (CSVRecord record : records) {
			  try { 
				  String target = record.get(pDateColumnIndex);
		          date =  df.parse(target);
		      } catch (Exception e) {
		          e.printStackTrace();
		      }
			  if (date.after(latest)){ 
				 latest=date;
			 }	 
		}
		if(in != null){
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(ERROR);
			}	
		}
		System.out.println(String.format("Latest date in file [%s] is: %s ", pFilename, latest.toString()));
		return latest;
	}
	
	public void sortCommentsListByDate(final List<CSVRecord> pRecords, final int pColumn){
		Collections.sort(pRecords, new Comparator<CSVRecord>(){
			DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss zzz yyyy");
			public int compare(CSVRecord r1, CSVRecord r2) {
				Date d1=null ,d2 = null;
				try {
					d1 = df.parse(r1.get(pColumn));
					d2 = df.parse(r2.get(pColumn));
				} catch (java.text.ParseException e) {
					e.printStackTrace();
				}
				return d2.compareTo(d1);
			}
	     });
		 System.out.println(String.format("Sorted %d records", pRecords.size()));
	}
	
	public void closePrinter(){
		if (mPrinter!=null){
			try {
				mPrinter.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(ERROR);
			}
		}
	}
		
	public void writeRecordData(List<CSVRecord> pRecords, String pFileName, CSVFormat pFormat){
		System.out.println(String.format("Writing %d new record(s) to file [%s] ", pRecords.size(), pFileName));
		try {
			mPrinter = new CSVPrinter(new PrintWriter(pFileName), pFormat);
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
			System.exit(ERROR);
		}	
		try {
			mPrinter.printRecords(pRecords);
		} catch (IOException e1) {
			  e1.printStackTrace();
		}
		closePrinter();	
	}
	
	public int getRatingFromEntry(SyndEntry pEntry){
		int rating = 0;
		@SuppressWarnings("unchecked")
		List<Element> foreignMarkups = (List<Element>) pEntry.getForeignMarkup();
		 for (Element foreignMarkup : foreignMarkups){
			 if (foreignMarkup.getName().equals("rating")) {
				rating = Integer.parseInt(foreignMarkup.getValue());
			 }
		 }
		 return rating;
	}
	
	private String getContent(SyndEntry pEntry){
		SyndContent content = (SyndContent) pEntry.getContents().get(0);
  		return content.getValue().replaceAll("\\n", "");
	}
	
	//====================================================EMAIL===============================================
	
	public void sendMail(final List<SyndEntry> pLowRatingsListSyndry, final String pFrom, String pTo[], final String pHost, 
												final String pSubject, int pAlertRating, final String pGoolgeAddress, final String pGooglePassword){
		  if (pLowRatingsListSyndry.size()==0) {
			  	System.out.println(String.format("No new comments with rating %d or below", pAlertRating));
			  return;
		  }
		  System.out.println("Sendiing mail...");
		  Properties props = new Properties();
	      props.put("mail.smtp.host", "smtp.gmail.com");
	      props.put("mail.smtp.auth", "true");
	      props.put("mail.debug", "true");
	      props.put("mail.smtp.port", 25);
	      props.put("mail.smtp.socketFactory.port", 25);
	      props.put("mail.smtp.starttls.enable", "true");
	      props.put("mail.transport.protocol", "smtp");
	      Session mailSession = null;
	      mailSession = Session.getInstance(props, new javax.mail.Authenticator() {
	            protected PasswordAuthentication getPasswordAuthentication() {
	                return new PasswordAuthentication(pGoolgeAddress, pGooglePassword);
	            }
	        });
	        try {
	            Transport transport = mailSession.getTransport();
	            MimeMessage message = new MimeMessage(mailSession);
	            message.setSubject(pSubject);
	            message.setFrom(new InternetAddress(pFrom));
	            for (String recipient : pTo){
	            	 message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
	            }
	            HtmlCanvas html = new HtmlCanvas()
	               .h4()
	                .content("Comments which have a rating less than or equal to " + pAlertRating)
	                 .table(new HtmlAttributes("border", "1"))
	                  .tr()
	                  .th().content("Rating")
	                  .th().content("Author")
	                  .th().content("Title")
	                  .th().content("Comment")
	                  .th().content("Date")
	                  ._tr();
	                  for (SyndEntry entry :pLowRatingsListSyndry){
	                	  html
	                	   	.tr()
	                	  		.td()
	                	  			.content(String.valueOf(getRatingFromEntry(entry)))
	                	  		.td()
	                	  			.content(entry.getAuthor())
	                	  		.td()
	                	  			.content(entry.getTitle())
	                	  		.td()
	                	  			.content(getContent(entry))
	                	  		.td()
	                	  			.content(entry.getUpdatedDate().toString())
	                	  	._tr();
	                  }	             	  
	                  html 
	                  ._table();
	            message.setContent(html.toHtml(),"text/html");
	            transport.connect();
	            transport.sendMessage(message,message.getRecipients(Message.RecipientType.TO));
	            transport.close();
	        } catch (Exception exception) {
	        	exception.printStackTrace();
	        }
	}
}
