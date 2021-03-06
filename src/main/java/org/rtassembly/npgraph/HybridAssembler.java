package org.rtassembly.npgraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import japsa.seq.Alphabet;
import japsa.seq.Sequence;
import japsa.seq.SequenceReader;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
@SuppressWarnings("restriction")
public class HybridAssembler {
    private static final Logger LOG = LoggerFactory.getLogger(HybridAssembler.class);
	//setting parameter for the GUI
    private boolean ready=false;
    private BooleanProperty overwrite, useSPAdesPath;
    private StringProperty 	prefix,
    						aligner,
    						alignerPath, 
							alignerOpt,
							shortReadsInput, 
							binReadsInput, 
							longReadsInput,
							shortReadsInputFormat, 
							longReadsInputFormat;
	
	Process alignmentProcess = null;
	private boolean stop=false;
	private String errorLog="";
	//Getters and Setters
	//==============================================================================================//
	public void setReady(boolean isReady) {ready=isReady;}
	public boolean getReady() {return ready;}
	
	public final void setOverwrite(boolean owr) {overwrite.set(owr);}
	public final boolean getOverwrite() {return overwrite.get();}
	public BooleanProperty overwriteProperty() {return overwrite;}
	
	public final void setUseSPAdesPath(boolean owr) {useSPAdesPath.set(owr);}
	public final boolean getUseSPAdesPath() {return useSPAdesPath.get();}
	public BooleanProperty useSPAdesPathProperty() {return useSPAdesPath;}
	
	public final void setPrefix(String output) {prefix.set(output);}
	public final String getPrefix() {return prefix.get();}
	public StringProperty prefixProperty() {return prefix;}
	
	public final void setAligner(String tool) {	aligner.set(tool);}
	public final String getAligner() {return aligner.get();}
	public StringProperty alignerProperty(){return aligner;}
	
	public final void setAlignerPath(String path) {alignerPath.set(path);}
	public final String getAlignerPath() {return alignerPath.get();}
	public StringProperty alignerPathProperty(){return alignerPath;}
	
	public final void setAlignerOpts(String setting) {alignerOpt.set(setting);}
	public final String getAlignerOpts() {return alignerOpt.get();}
	public StringProperty alignerOptProperty(){return alignerOpt;}
	
	public final void setErrorLog(String log) {errorLog=log;}
	public final String getErrorLog() {return errorLog;}
	
	public final String getFullPathOfAligner() {return getAlignerPath()+"/"+getAligner();}
	
	public final void setBinReadsInput(String brInput) {binReadsInput.set(brInput);}
	public final String getBinReadsInput() {return binReadsInput.get();}
	public StringProperty binReadsInputProperty() {return binReadsInput;}
	
	public final void setShortReadsInput(String srInput) {
		shortReadsInput.set(srInput);
	}
	public final String getShortReadsInput() {return shortReadsInput.get();}
	public StringProperty shortReadsInputProperty() {return shortReadsInput;}
	
	public final void setLongReadsInput(String lrInput) {
		longReadsInput.set(lrInput);
	}
	public final String getLongReadsInput() {return longReadsInput.get();}
	public StringProperty longReadsInputProperty() {return longReadsInput;}
	
	public final void setShortReadsInputFormat(String srInputFormat) {shortReadsInputFormat.set(srInputFormat);}
	public final String getShortReadsInputFormat() {return shortReadsInputFormat.get();}
	public StringProperty shortReadsInputFormatProperty() {return shortReadsInputFormat;}
	
	public final void setLongReadsInputFormat(String lrInputFormat) {
		lrInputFormat=lrInputFormat.toLowerCase();
		if(	lrInputFormat.contains("fasta") || lrInputFormat.contains("fa") || lrInputFormat.contains("fna")
			|| lrInputFormat.contains("fastq") || lrInputFormat.contains("fq"))
			longReadsInputFormat.set("fasta/fastq");
		else if(lrInputFormat.contains("sam") || lrInputFormat.contains("bam"))
			longReadsInputFormat.set("sam/bam");
	}
	public final String getLongReadsInputFormat() {return longReadsInputFormat.get();}
	public StringProperty longReadsInputFormatProperty() {return longReadsInputFormat;}
	
	public synchronized void setStopSignal(boolean stop) {this.stop=stop;}
	public synchronized boolean getStopSignal() {return stop;}
	//===============================================================================================//
	
	//Operational variables
//	final BDGraph origGraph;
	public BDGraph simGraph; //original and simplified graph should be separated, no???
	public GraphWatcher observer;
	public static boolean VERBOSE=false;
	public HybridAssembler(){
//		origGraph=new BDGraph("batch");
		simGraph=new BDGraph("real");
//		rtComponents = new ConnectedComponents();
		simGraph.setAttribute("ui.quality");
		simGraph.setAttribute("ui.antialias");
		
		overwrite = new SimpleBooleanProperty(true);
		useSPAdesPath = new SimpleBooleanProperty(false);
	    prefix = new SimpleStringProperty("/tmp/");
	    aligner = new SimpleStringProperty("");
		alignerPath = new SimpleStringProperty(""); 
		alignerOpt = new SimpleStringProperty("");
		
		shortReadsInput = new SimpleStringProperty(""); 
		binReadsInput = new SimpleStringProperty(""); 
		longReadsInput = new SimpleStringProperty("");
		shortReadsInputFormat = new SimpleStringProperty(""); 
		longReadsInputFormat = new SimpleStringProperty("");
		
		//set all binding options here...
        shortReadsInput.addListener((observable, oldValue, newValue) -> 
			{
				String fn = ((String)observable.getValue()).toLowerCase();
				if(	fn.endsWith(".fastg")) 
					setShortReadsInputFormat("fastg");
				else if(fn.endsWith(".gfa"))
					setShortReadsInputFormat("gfa");
			}	 

        );
        
        shortReadsInputFormat.addListener((observable, oldValue, newValue) -> 
			{
				if(!getShortReadsInput().toLowerCase().endsWith(newValue))
					setShortReadsInput("");
			}	 

        );
		
        longReadsInput.addListener( (observable, oldValue, newValue) -> 
    		{
				String fn = ((String)observable.getValue()).toLowerCase();
				if(	fn.endsWith(".fasta") || fn.endsWith(".fa") || fn.endsWith("fna")
					|| fn.endsWith(".fastq") || fn.endsWith(".fq")
					|| fn.endsWith(".fasta.gz") || fn.endsWith(".fa.gz") || fn.endsWith("fna.gz")
					|| fn.endsWith(".fastq.gz") || fn.endsWith(".fq.gz") 
					) 
					setLongReadsInputFormat("fasta/fastq");
				else if(fn.endsWith(".sam") || fn.endsWith(".bam")) 
					setLongReadsInputFormat("sam/bam");		
    		}	 
        );
        
        longReadsInputFormat.addListener((observable, oldValue, newValue) -> 
			{
				String oldFile=getLongReadsInput().toLowerCase();
				if(oldFile.equals("-"))
					return;
				if(	newValue.equals("fasta/fastq") 
							&& !oldFile.endsWith(".fasta") && !oldFile.endsWith(".fa") && !oldFile.endsWith("fna")
							&& !oldFile.endsWith(".fastq") && !oldFile.endsWith(".fq")
							&& !oldFile.endsWith(".fasta.gz") && !oldFile.endsWith(".fa.gz") && !oldFile.endsWith("fna.gz")
							&& !oldFile.endsWith(".fastq.gz") && !oldFile.endsWith(".fq.gz") 
							) 
					setLongReadsInput("");
						
				if(newValue.equals("sam/bam") && !oldFile.endsWith(".sam") && !oldFile.endsWith(".bam"))
					setLongReadsInput("");
			}	 

        );
        
        aligner.addListener( (observable, oldValue, newValue) ->
        	{
				String aligner=(String)observable.getValue();
				if(aligner.toLowerCase().equals("minimap2"))
					setAlignerOpts("-t4 -k15 -w5");
				else if (aligner.toLowerCase().equals("bwa"))
					setAlignerOpts("-t4 -k11 -W20 -r10 -A1 -B1 -O1 -E1 -L0 -a -Y");			
			}	 

        );
        

	}
	
    private boolean checkFile(String _path) {
		if (_path.equals("")){
			setErrorLog("Empty file \"" + _path + "\"");
			return false;
		}
		File _file = new File(_path);
		if (!_file.isFile()){
			setErrorLog("File \"" + _path + "\" is not valid!");
			return false;
		}
		return true;
    }
    private boolean checkFolder(String _path) {
		if (_path.equals("")){
			setErrorLog("Empty directory \"" + _path + "\"");
			return false;
		}
		File _file = new File(_path);
		if (!_file.isDirectory()){
			setErrorLog("Directory \"" + _path + "\" is not valid!");
			return false;
		}
		return true;
    }	
	
	//Indexing reference, prepare for alignment...
	public boolean prepareLongReadsProcess(){
		if(!getLongReadsInputFormat().equals("fasta/fastq") && !getLongReadsInputFormat().equals("sam/bam")){
			setErrorLog("Please specify a correct format of long read data (FASTA/FASTQ or BAM/SAM)!");
			return false;
		}
		if(!getLongReadsInput().equals("-") && !checkFile(getLongReadsInput()))
			return false;
		
		if(!checkFolder(getPrefix()))
			return false;
		try{
			System.setProperty("usr.dir", getPrefix());
		}
		catch(NullPointerException | IllegalArgumentException | SecurityException exception ){
			setErrorLog("Fail to set working directory usr.dir to " + getPrefix());
			return false;
		}
		
		//if long reads data not given in SAM/BAM, need to invoke minimap2
        if(getLongReadsInputFormat().toLowerCase().startsWith("fast")) {
        	File indexFile=null;
        	ArrayList<String> idxCmd = new ArrayList<>();
        	idxCmd.add(getFullPathOfAligner());
        	if(getAligner().equals("minimap2")) { 	
				indexFile=new File(getPrefix()+"/assembly_graph.mmi");												
				if(!checkMinimap2()) 
						return false;
				idxCmd.addAll(Arrays.asList(getAlignerOpts().split("\\s")));
				idxCmd.add("-d");
				idxCmd.add(getPrefix()+"/assembly_graph.mmi");
														
        	}else if(getAligner().equals("bwa")) {
				indexFile=new File(getPrefix()+"/assembly_graph.fasta.bwt");
				if(!checkBWA()) 
						return false;
				idxCmd.add("index");

        	}else {
        		setErrorLog("Invalide aligner! Set to BWA or minimap2 please!");
        		return false;
        	}
			idxCmd.add(getPrefix()+"/assembly_graph.fasta");

			if(getOverwrite() || !indexFile.exists()) {						
				try{
					simGraph.outputFASTA(getPrefix()+"/assembly_graph.fasta");
					
					ProcessBuilder pb = new ProcessBuilder(idxCmd);
					Process indexProcess =  pb.start();
					indexProcess.waitFor();
					
				}catch (IOException | InterruptedException e){
					setErrorLog("Issue when indexing the pre-assemblies: \n" + e.getMessage());
					return false;
				}
			}
			
        }
        return true;
	}
	//Loading the graph, doing preprocessing
	//binning, ...
	public boolean prepareShortReadsProcess() {
		if(!getShortReadsInputFormat().equals("fastg") && !getShortReadsInputFormat().equals("gfa")){
			setErrorLog("Please specify a correct format of graph file!");
			return false;
		}
			
		if(!checkFile(getShortReadsInput()))
			return false;
		
		//try to read input file
		try {
			if(getShortReadsInputFormat().toLowerCase().equals("gfa")) 
				GraphUtil.loadFromGFA(getShortReadsInput(), getBinReadsInput(), simGraph, getUseSPAdesPath());
			else if(getShortReadsInputFormat().toLowerCase().equals("fastg"))
				GraphUtil.loadFromFASTG(getShortReadsInput(), getBinReadsInput(), simGraph, getUseSPAdesPath());
			else 				
				throw new IOException("Assembly graph file must have .gfa or .fastg extension!");
			
		}catch(IOException e) {
			setErrorLog("Issue when loading pre-assembly: \n" + e.getMessage());
			return false;
		}
		
		
		simGraph.updateStats();
		observer = new GraphWatcher(simGraph);
		return true;
	}

	/**
	 * SHN modified the default aligner to minimap2
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void assembly() 
			throws IOException, InterruptedException{

		LOG.info("Scaffolding ready at {}", new Date());

		SamReaderFactory.setDefaultValidationStringency(ValidationStringency.SILENT);
		SamReader reader = null;

		if (getLongReadsInputFormat().endsWith("am")){//bam or sam
			if ("-".equals(getLongReadsInput()))
				reader = SamReaderFactory.makeDefault().open(SamInputResource.of(System.in));
			else
				reader = SamReaderFactory.makeDefault().open(new File(getLongReadsInput()));	
		}else{
			LOG.info("Starting alignment by {} at {}", getAligner(), new Date());
			ProcessBuilder pb = null;
			List<String> command = new ArrayList<>();
			command.add(getFullPathOfAligner());
			if(getAligner().equals("minimap2")) {
				command.add("-a");
				command.addAll(Arrays.asList(getAlignerOpts().split("\\s")));
				command.add("-K20000");
				command.add(getPrefix()+"/assembly_graph.mmi");
				command.add(getLongReadsInput());
			}
			else if(getAligner().equals("bwa")) {
				command.add("mem");
				command.addAll(Arrays.asList(getAlignerOpts().split("\\s")));
				command.add("-K20000");
				command.add(getPrefix()+"/assembly_graph.fasta");
				command.add(getLongReadsInput());
			}
			
			if ("-".equals(getLongReadsInput())){
				pb = new ProcessBuilder(command).redirectInput(Redirect.INHERIT);
			}else{
				pb = new ProcessBuilder(command);
			}

//			alignmentProcess  = pb.redirectError(ProcessBuilder.Redirect.to(new File("/dev/null"))).start();
			alignmentProcess  = pb.redirectError(ProcessBuilder.Redirect.to(new File(getPrefix()+"/alignment.log"))).start();

			LOG.info("{} started!", getAligner());			

			reader = SamReaderFactory.makeDefault().open(SamInputResource.of(alignmentProcess.getInputStream()));

		}
		SAMRecordIterator iter = reader.iterator();

		String readID = "";
		Sequence nnpRead = null;
		ArrayList<Alignment> samList =  new ArrayList<Alignment>();// alignment record of the same read;	
		SAMRecord rec=null;
		while (iter.hasNext()) {
			if(getStopSignal())
				break;
			
			try {
				rec = iter.next();
			}catch(Exception e) {
				if(HybridAssembler.VERBOSE) {
					LOG.warn("Ignore one faulty SAM record: \n {}", e.getMessage());
					e.printStackTrace();
				}
					
				continue;
			}
			
			if (rec.getReadUnmappedFlag())
				continue;
			
			if (rec.getMappingQuality() < Alignment.MIN_QUAL)
				continue;
			
			String refName = rec.getReferenceName();
			String refID = refName.split("_").length > 1 ? refName.split("_")[1]:refName;
			
			if (simGraph.getNode(refID)==null) {
				if(HybridAssembler.VERBOSE)
					LOG.warn("Node {} not found from the graph!", refID);
				continue;
			}
			Alignment myRec = new Alignment(rec, (BDNode) simGraph.getNode(refID)); 

			//////////////////////////////////////////////////////////////////
			
			if (!readID.equals("") && !readID.equals(myRec.readID)) {	
				synchronized(simGraph) {
					List<BDPath> paths=simGraph.uniqueBridgesFinding(nnpRead, samList);
					if(paths!=null){	
						for(BDPath path:paths) 
						{
							//path here is already unique! (2 unique ending nodes)
					    	if(simGraph.reduceUniquePath(path)) {
					    		observer.update(false);					    		
					    	}
						}
					}
				}
				samList = new ArrayList<Alignment>();
				nnpRead = new Sequence(Alphabet.DNA5(), rec.getReadString(), "R" + readID);
			}	
			readID = myRec.readID;
			samList.add(myRec); 
		}// while
		iter.close();
		reader.close();

		terminateAlignmentProcess();	

	}
	
	public void terminateAlignmentProcess() {
		if (alignmentProcess != null){
			alignmentProcess.destroy();
		}	
	}
	
	public void postProcessGraph() throws IOException{
		//Take the current best path among the candidate of a bridge and connect the bridge(greedy)
		for(GoInBetweenBridge brg:simGraph.getUnsolvedBridges()){
			if(HybridAssembler.VERBOSE)
				LOG.info("Last attempt on incomplete bridge {} : anchors={} \n {}", brg.getEndingsID(), brg.getNumberOfAnchors(), brg.getAllPossiblePaths());
			
			if(brg.getCompletionLevel()>=3) 
				simGraph.chopPathAtAnchors(brg.getBestPath(brg.pBridge.getNode0(),brg.pBridge.getNode1())).stream().forEach(p->simGraph.reduceUniquePath(p));
			else{
				brg.scanForAnEnd(true);
				//selective connecting
				brg.steps.connectBridgeSteps(true);
				//return appropriate path
				if(brg.segments!=null)
					simGraph.chopPathAtAnchors(brg.getBestPath(brg.steps.start.getNode(),brg.steps.end.getNode())).stream().forEach(p->simGraph.reduceUniquePath(p));
				else if(HybridAssembler.VERBOSE)
					LOG.info("Last attempt failed");
			}


		}
		
        //update for the last time
        observer.update(true);
		observer.outputFASTA(getPrefix()+"/npgraph_assembly.fasta");
		observer.outputGFA(getPrefix()+"/npgraph_assembly.gfa");

	}
	


    
    @SuppressWarnings("resource")
	public static void promptEnterKey(){
    	   LOG.info("Press \"ENTER\" to continue...");
    	   Scanner scanner = new Scanner(System.in);
    	   scanner.nextLine();
    	}
    
    
    public boolean checkMinimap2() {    		
		ProcessBuilder pb = new ProcessBuilder(getFullPathOfAligner(),"-V").redirectErrorStream(true);
		Process process;
		try {
			process = pb.start();
			BufferedReader bf = SequenceReader.openInputStream(process.getInputStream());
	
	
			String line;
			String version = "";
			Pattern versionPattern = Pattern.compile("^(\\d+\\.\\d+).*");
			Matcher matcher=versionPattern.matcher("");
			
			while ((line = bf.readLine())!=null){				
				matcher.reset(line);
				if (matcher.find()){
				    version = matcher.group(1);
				    break;//while
				}
				
								
			}	
			bf.close();
			
			if (version.length() == 0){
				setErrorLog("Command " + getFullPathOfAligner() + " -V doesn't give version info. Check version failed!");
				return false;
			}else{
				LOG.info("minimap version: " + version);
				if (version.compareTo("2.0") < 0){
					setErrorLog("Require minimap version 2 or above!");
					return false;
				}
			}
		} catch (IOException e) {
			setErrorLog("Error running: " + getFullPathOfAligner() + "\n" + e.getMessage());
			return false;
		}
		
		return true;
			
    }
    
    public boolean checkBWA() {    		
		try{
			ProcessBuilder pb = new ProcessBuilder(getFullPathOfAligner()).redirectErrorStream(true);
			Process process =  pb.start();
			BufferedReader bf = SequenceReader.openInputStream(process.getInputStream());


			String line;
			String version = "";
			Pattern versionPattern = Pattern.compile("^Version:\\s(\\d+\\.\\d+\\.\\d+).*");
			Matcher matcher=versionPattern.matcher("");
			
			while ((line = bf.readLine())!=null){				
				matcher.reset(line);
				if (matcher.find()){
				    version = matcher.group(1);
				    break;//while
				}
				
								
			}	
			bf.close();
			
			if (version.length() == 0){
				setErrorLog("Command " + getFullPathOfAligner() + " doesn't give version info. Check version failed!");
				return false;
			}else{
				LOG.info("bwa version: " + version);
				if (version.compareTo("0.7.11") < 0){
					setErrorLog(" Require bwa of 0.7.11 or above");
					return false;
				}
			}

		}catch (IOException e){
			setErrorLog("Error running: " + getFullPathOfAligner() + "\n" + e.getMessage());
			return false;
		}
		
		return true;
			
    }

	public static void main(String[] argv) throws IOException, InterruptedException{
		HybridAssembler hbAss = new HybridAssembler();
		
		hbAss.setShortReadsInput("/home/sonhoanghguyen/Projects/scaffolding/data/spades_3.7/EcK12S-careful/assembly_graph.fastg");
		hbAss.setShortReadsInputFormat("fastg");
		hbAss.prepareShortReadsProcess();
		hbAss.setLongReadsInput("/home/sonhoanghguyen/Projects/scaffolding/data/spades_3.7/EcK12S-careful/assembly_graph.sam");
		hbAss.setLongReadsInputFormat("sam/bam");
		hbAss.prepareLongReadsProcess();
		hbAss.assembly();

	}
	
}
