import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;

import ro.racai.base.Sentence;
import ro.racai.base.Token;
import ro.racai.conllup.CONLLUPReader;
import ro.racai.conllup.CONLLUPWriter;

// -Djava.util.concurrent.ForkJoinPool.common.parallelism=10

public class MakeCorpus {
	
	private static String outPath; 
	
	private final static boolean DEBUG=false;
	
	private static class Status {
		public boolean ok;
	}
	
	public static boolean checkWord(String word, String value, String type, Token prevTok, Status status) {
		if(word.equals(value)) {
			if(prevTok==null || prevTok.getByKey("RACAI:PUNCTUATION")!=null) {
				if(DEBUG) {
					if(prevTok==null)System.out.println("prevTok=null");
					else System.out.println("PUNCTUATION already set");
					System.out.println("tok=["+word+"]");
				}
				status.ok=false;
			}else {
				prevTok.setByKey("RACAI:PUNCTUATION", type);
			}
			return true;
		}
		return false;
	}
	
	public static void expandToken(Token tok) {
		String word=tok.getByKey("FORM");
		if(word.equals("nr.")) {tok.setByKey("FORM", "numărul");tok.setByKey("LEMMA", "număr");}
		else if(word.equalsIgnoreCase("nr.")) {tok.setByKey("FORM", "Numărul");tok.setByKey("LEMMA", "număr");}
	}
	
	public static void processSingleFile(Path fpathIn, Path fpathOut) throws Exception {
		
		System.out.println(String.format("Process [%s] => [%s]",fpathIn.toString(),fpathOut.toString()));
		
		CONLLUPReader in=new CONLLUPReader(fpathIn);
		CONLLUPWriter out=null;
		Status status=new Status();
		
		for(Sentence sentIn=in.readSentence();sentIn!=null;sentIn=in.readSentence()) {
			Sentence sentOut=new Sentence();
			for(String s:sentIn.getMetaOrder())sentOut.setMetaValue(s, sentIn.getMetaValue(s));
			
			if(DEBUG)System.out.println(sentIn.getMetaValue("text"));
			
			Token prevTok=null;
			status.ok=true;
			for(Token tok:sentIn.getTokens()) {
				expandToken(tok);
				
				String word=tok.getByKey("FORM");
				String upos=tok.getByKey("UPOS");
				
				if(
					!checkWord(word,".","PeriodAfter=yes",prevTok,status) &&
					!checkWord(word,",","CommaAfter=yes",prevTok,status) &&
					//!checkWord(word,"?","QuestionAfter=yes",prevTok,status) &&
					//!checkWord(word,"!","ExclamationAfter=yes",prevTok,status) &&
					!checkWord(word,";","SemicolonAfter=yes",prevTok,status) &&
					!checkWord(word,":","ColonAfter=yes",prevTok,status) &&
					!checkWord(word,"-","HyphenAfter=yes",prevTok,status)
				) {
					if(upos.equals("PUNCT") || upos.equals("SYM")){
						if(DEBUG)System.out.println("BREAK 1");
						status.ok=false;
					}else {
						if(!word.matches("^[-_a-zA-Z0-9ăîâșțĂÎÂȘȚ]*$")) {
							status.ok=false; 
							if(DEBUG)System.out.println("BREAK 2");
							break;
						}
						if(word.indexOf('-')>=0 || word.indexOf('_')>=0) {
							Token lastTok=null;
							String w1="";
							for(int i=0;i<word.length();i++) {
								if(word.charAt(i)=='-' || word.charAt(i)=='_') {
									if(w1.length()>0) {
										Token t1=tok.clone();
										t1.setByKey("FORM", w1);
										t1.setByKey("LEMMA", w1);
										if(word.charAt(i)=='-')t1.setByKey("RACAI:PUNCTUATION", "HyphenAfter=yes");
										else t1.setByKey("RACAI:PUNCTUATION", "_");
										if(lastTok!=null)sentOut.addToken(lastTok);
										lastTok=t1;
										w1="";
									}
								}else w1+=word.charAt(i);
							}
							
							if(w1.length()>0) {
								if(lastTok!=null)sentOut.addToken(lastTok);
								tok.setByKey("FORM", w1);
								tok.setByKey("LEMMA", w1);
							}else tok=lastTok;
						}
						
						if(tok==null)break;
						
						sentOut.addToken(tok);
						if(prevTok!=null && prevTok.getByKey("RACAI:PUNCTUATION")==null) {
							prevTok.setByKey("RACAI:PUNCTUATION", "_");
						}
						prevTok=tok;
					}
				}
				
				if(!status.ok) {
					if(DEBUG)System.out.println("BREAK 3");
					break;
				}
				
				
			}
			
			if(status.ok && sentOut.getTokens().size()>3) {
				Token lastTok=sentOut.getToken(sentOut.getTokens().size()-1);
				String punct=lastTok.getByKey("RACAI:PUNCTUATION");
				if(punct==null) punct="_";
				if(punct.equals("PeriodAfter=yes") || punct.equals("QuestionAfter=yes") || punct.equals("ExclamationAfter=yes")) {
				
					if(out==null) {
						ArrayList<String> columns=new ArrayList<>(10);
						for(int i=0;i<10;i++)columns.add(in.getColumns().get(i));
						columns.add("RACAI:PUNCTUATION");
						out=new CONLLUPWriter(fpathOut, in.getMetaOrder(), sentIn.getMetaOrder(), columns);
					}
					
					out.writeSentence(sentOut);
				}else {
					if(DEBUG)System.out.println("BREAK 4");
				}
			}
		}
		
		if(out!=null)out.close();
		
		in.close();
	}
	
	public static void processSingleFileNoException(Path fpathIn) {
		try {
			Path fpathOut=Paths.get(outPath, fpathIn.getFileName().toString());
			processSingleFile(fpathIn,fpathOut);
		}catch(Exception ex) {
			System.out.println(String.format("[%s] %s", fpathIn.toString(),ex.getMessage()));
			ex.printStackTrace();
		}
	}	
	
	
	public static void main(String[] args) throws Exception {
		if(args.length!=3) {
			System.out.println("Syntax:");
			System.out.println("    MakeCorpus <EXTENSION> <PATH_IN> <PATH_OUT>");
			System.out.println("");
			System.out.println("      EXTENSION = file extension");
			System.out.println("      PATH_IN   = input folder");
			System.out.println("      PATH_OUT  = output folder");
			System.out.println("");
			System.out.println("Example:");
			System.out.println("    MakeCorpus conllup corpus/in corpus/out");
			System.out.println("");
			System.exit(-1);
		}
		
		outPath=args[2];
		Stream<Path> paths = Files.walk(Paths.get(args[1]),1);
		paths.parallel()
			.filter(Files::isRegularFile)
			.filter(x->x.toString().endsWith(args[0]))
			.forEach(MakeCorpus::processSingleFileNoException);
		paths.close();
	}

}
