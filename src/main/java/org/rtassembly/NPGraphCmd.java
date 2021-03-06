package org.rtassembly;
import java.io.File;
import java.io.IOException;
import org.rtassembly.gui.NPGraphFX;
import org.rtassembly.npgraph.Alignment;
import org.rtassembly.npgraph.BDGraph;
import org.rtassembly.npgraph.HybridAssembler;
import org.rtassembly.npgraph.SimpleBinner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import japsa.util.CommandLine;
import javafx.application.Application;



@SuppressWarnings("restriction")
public class NPGraphCmd extends CommandLine{
    private static final Logger LOG = LoggerFactory.getLogger(NPGraphCmd.class);
	public NPGraphCmd(){
		super();

		addString("si", "", "Name of the short-read assembly file.");
		addString("sf", "", "Format of the assembly input file. Accepted format are FASTG, GFA");
		addString("li", "", "Name of the long-read data input file, - for stdin.");
		addString("lf", "", "Format of the long-read data input file. This may be FASTQ/FASTA (MinION reads) or SAM/BAM (aligned with the assembly graph already)");
		addString("output", "/tmp/", "Output folder for temporary files and the final assembly npgraph_assembly.fasta");
				
		addString("sb", "", "Name of the metaBAT file for binning information (experimental).");

		addString("aligner","","Aligner tool that will be used, either minimap2 or bwa");

		addString("algPath","","Absolute path to the binary aligner file");
		addString("algOpt", "", "Settings used by aligner to align long reads to the contigs");
		
		addBoolean("overwrite", true, "Whether to overwrite or reuse the intermediate file");

		addBoolean("sp", false, "Whether to use SPAdes contigs.paths for bridging.");
		addInt("qual", 10, "Minimum quality of alignment to considered");
		addInt("mcov", 3, "Minimum number of reads spanning a confident bridge");

		addBoolean("gui", false, "Whether using GUI or not.");
		addBoolean("verbose", false, "For debugging.");
		addStdHelp();
	}
	
	public static void main(String[] args) throws IOException{
		CommandLine cmdLine = new NPGraphCmd();		
		args = cmdLine.stdParseLine(args);

		/***********************************************************************/
		String 	shortReadsInput = cmdLine.getStringVal("si"),
				shortReadsInputFormat = cmdLine.getStringVal("sf"),
				longReadsInput = cmdLine.getStringVal("li"),
				longReadsInputFormat = cmdLine.getStringVal("lf"),
				outputDir = cmdLine.getStringVal("output"),
				shortReadsBinInput = cmdLine.getStringVal("sb"),
				alg=cmdLine.getStringVal("aligner"),
				algPath = cmdLine.getStringVal("algPath"),
				algOpt = cmdLine.getStringVal("algOpt");
		boolean overwrite = cmdLine.getBooleanVal("overwrite"),
				spaths = cmdLine.getBooleanVal("sp"),
				gui = cmdLine.getBooleanVal("gui");
			
		Alignment.MIN_QUAL = cmdLine.getIntVal("qual");
		BDGraph.MIN_COVER=cmdLine.getIntVal("mcov");
		HybridAssembler.VERBOSE=cmdLine.getBooleanVal("verbose");
		//Default output dir 
		if(outputDir == null) {
			outputDir = new File(shortReadsInput).getAbsoluteFile().getParent();
		}
		File outDir = new File(outputDir);
		if(!outDir.exists())
			outDir.mkdirs();
			
		//1. Create an assembler object with appropriate file loader
		HybridAssembler hbAss = new HybridAssembler();
		if(shortReadsInput!=null && !shortReadsInput.isEmpty())
			hbAss.setShortReadsInput(shortReadsInput);
		if(shortReadsInputFormat!=null && !shortReadsInputFormat.isEmpty())
			hbAss.setShortReadsInputFormat(shortReadsInputFormat);
		if(longReadsInput!=null && !longReadsInput.isEmpty())
			hbAss.setLongReadsInput(longReadsInput);
		if(longReadsInputFormat!=null && !longReadsInputFormat.isEmpty())
			hbAss.setLongReadsInputFormat(longReadsInputFormat);
		
		hbAss.setPrefix(outputDir);
		if(shortReadsBinInput!=null && !shortReadsBinInput.isEmpty())
			hbAss.setBinReadsInput(shortReadsBinInput);
		
		if(alg!=null && !alg.isEmpty())
			hbAss.setAligner(alg);
		if(algPath!=null && !algPath.isEmpty())
			hbAss.setAlignerPath(algPath);
		if(algOpt!=null && !algOpt.isEmpty())
			hbAss.setAlignerOpts(algOpt);
		
		hbAss.setOverwrite(overwrite);
		hbAss.setUseSPAdesPath(spaths);
		        
		//4. Call the assembly function or invoke GUI to do so
        if(gui) {
			NPGraphFX.setAssembler(hbAss);
			Application.launch(NPGraphFX.class,args);
        }else {
	        
			try {
				if(hbAss.prepareShortReadsProcess() &&	hbAss.prepareLongReadsProcess()) {
					hbAss.assembly();
					hbAss.postProcessGraph();
				}
				else{
					LOG.error("Error with pre-processing step: \n" + hbAss.getErrorLog());
					System.exit(1);
				}
					
			} catch (InterruptedException|IOException e) {
				LOG.error("Issue when assembly: \n" + e.getMessage());
				e.printStackTrace();
				System.exit(1);
			}
        }
		
	}
	
}
