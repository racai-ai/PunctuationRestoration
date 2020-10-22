import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import ro.racai.base.Sentence;
import ro.racai.base.Token;
import ro.racai.conllup.CONLLUPReader;

// -Djava.util.concurrent.ForkJoinPool.common.parallelism=10

public class Stats {
	
	private static class Numbers {
		int period;
		int comma;
		int hyphen;
		int colon;
		int question;
		int exclamation;
		int semicolon;
		int sentences;
		int tokens;
		int files;
		
		public Numbers() {
			period=0; comma=0; hyphen=0; colon=0; question=0; exclamation=0; semicolon=0;
			sentences=0; tokens=0; files=0;
		}
		
		public synchronized void incPeriod() {period++;}
		public synchronized void incComma() {comma++;}
		public synchronized void incHyphen() {hyphen++;}
		public synchronized void incColon() {colon++;}
		public synchronized void incQuestion() {question++;}
		public synchronized void incExclamation() {exclamation++;}
		public synchronized void incSemicolon() {semicolon++;}
		public synchronized void incSentences() {sentences++;}
		public synchronized void incTokens() {tokens++;}
		public synchronized void incFiles() {files++;}

		/**
		 * @return the period
		 */
		public int getPeriod() {
			return period;
		}

		/**
		 * @return the comma
		 */
		public int getComma() {
			return comma;
		}

		/**
		 * @return the hyphen
		 */
		public int getHyphen() {
			return hyphen;
		}

		/**
		 * @return the colon
		 */
		public int getColon() {
			return colon;
		}

		/**
		 * @return the question
		 */
		public int getQuestion() {
			return question;
		}

		/**
		 * @return the exclamation
		 */
		public int getExclamation() {
			return exclamation;
		}

		/**
		 * @return the semicolon
		 */
		public int getSemicolon() {
			return semicolon;
		}

		/**
		 * @return the sentences
		 */
		public int getSentences() {
			return sentences;
		}
		/**
		 * @return the tokens
		 */
		public int getTokens() {
			return tokens;
		}
		/**
		 * @return the tokens
		 */
		public int getFiles() {
			return files;
		}
	}
	
	private static Numbers numbers=new Numbers();
	
	public static void processSingleFile(Path fpathIn) throws Exception {
		
		System.out.println(String.format("Process [%s]",fpathIn.toString()));
		
		CONLLUPReader in=new CONLLUPReader(fpathIn);
		numbers.incFiles();
		for(Sentence sentIn=in.readSentence();sentIn!=null;sentIn=in.readSentence()) {
			numbers.incSentences();
			for(Token tok:sentIn.getTokens()) {
				numbers.incTokens();
				String punct=tok.getByKey("RACAI:PUNCTUATION");
				if(punct==null)continue;
				
				if(punct.indexOf("PeriodAfter=yes")>=0)numbers.incPeriod();
				else if(punct.indexOf("CommaAfter=yes")>=0)numbers.incComma();
				else if(punct.indexOf("QuestionAfter=yes")>=0)numbers.incQuestion();
				else if(punct.indexOf("ExclamationAfter=yes")>=0)numbers.incExclamation();
				else if(punct.indexOf("SemicolonAfter=yes")>=0)numbers.incSemicolon();
				else if(punct.indexOf("ColonAfter=yes")>=0)numbers.incColon();
				else if(punct.indexOf("HyphenAfter=yes")>=0)numbers.incHyphen();
			}
		}
		in.close();
	}
	
	public static void processSingleFileNoException(Path fpathIn) {
		try {
			processSingleFile(fpathIn);
		}catch(Exception ex) {
			System.out.println(String.format("[%s] %s", fpathIn.toString(),ex.getMessage()));
			ex.printStackTrace();
		}
	}	
	
	
	public static void main(String[] args) throws Exception {
		if(args.length!=2) {
			System.out.println("This program should be executed after MakeCorpus.");
			System.out.println("It will count the different values present in the field RACAI:PUNCTUATION");
			System.out.println("");
			System.out.println("Syntax:");
			System.out.println("    Stats <EXTENSION> <PATH_IN>");
			System.out.println("");
			System.out.println("      EXTENSION = file extension");
			System.out.println("      PATH_IN   = input folder");
			System.out.println("");
			System.out.println("Example:");
			System.out.println("    Stats conllup corpus/out");
			System.out.println("");
			System.exit(-1);
		}
		
		Stream<Path> paths = Files.walk(Paths.get(args[1]),1);
		paths.parallel()
			.filter(Files::isRegularFile)
			.filter(x->x.toString().endsWith(args[0]))
			.forEach(Stats::processSingleFileNoException);
		paths.close();
		
		System.out.println("period\t"+numbers.getPeriod());
		System.out.println("question\t"+numbers.getQuestion());
		System.out.println("exclamation\t"+numbers.getExclamation());
		System.out.println("comma\t"+numbers.getComma());
		System.out.println("colon\t"+numbers.getColon());
		System.out.println("semicolon\t"+numbers.getSemicolon());
		System.out.println("hyphen\t"+numbers.getHyphen());
		System.out.println("files\t"+numbers.getFiles());
		System.out.println("sentences\t"+numbers.getSentences());
		System.out.println("tokens\t"+numbers.getTokens());
		
	}

}
