import java.io.*;
import java.util.*;

/** Performs LZW compression algorithm on most file types. This implementation
 *  uses 9 - 16 bits for the dictionary entries. Once it hits 2^16 entries, the 
 *  dictionary stops adding in new entries and uses only the existing ones.
 *  
 * @author Alec J. Horne
 *
 */
 
public class LZW2 {
	/** Compress a file of any type. */
    public static void compress(String p) {
    	System.out.println("Starting compression...");
    	String fileName = p.substring(0, p.indexOf('.'));
    	String fileType = p.substring(p.indexOf('.'), p.length());
    	 
    	//read the data from the file
    	DataInputStream dis = null;
    	InputStream is = null;
    	byte data[] = null;
    	String uncompressed = "";
    	try {
    		is = new FileInputStream(p);
    		dis = new DataInputStream(is);
    		int length = dis.available();
            data = new byte[length];
    		dis.readFully(data);
    		//encode string to base64 if image file
    		if(fileType.compareToIgnoreCase(".gif") == 0 || fileType.compareToIgnoreCase(".tiff") == 0
    				|| fileType.compareToIgnoreCase(".docx") == 0)
    			uncompressed = Base64.getEncoder().encodeToString(data);
    		else
    			uncompressed = new String(data);
    		dis.close();
    	} catch(Exception e){
    		e.printStackTrace();
    	}

    	// Build the dictionary.
        int dictSize = 256;
        Map<String,Integer> dictionary = new HashMap<String,Integer>();
        //Add first 256 entries as extended ascii table chars
        for (int i = 0; i < 256; i++)
            dictionary.put("" + (char)i, i);
        
        String w = "";
        int bits = 9;
        List<Integer> result = new ArrayList<Integer>();
        for (char c : uncompressed.toCharArray()) {
        	// Add the code to increase the bit size
        	if(dictSize == (int)(Math.pow(2, bits) - 1) && bits != 16){
        		result.add((int)Math.pow(2, bits) - 1);
        		bits++;
        	}
           	String wc = w + c;
           	if (dictionary.containsKey(wc))
               	w = wc;
           	else {
       			result.add(dictionary.get(w));
       			w = "" + c;
       			// Add wc to the dictionary until the dictionary size is 16 bits
       			if(dictSize < (Math.pow(2, 16) - 1))
       				dictionary.put(wc, dictSize++);
       		}
        }
        
        // Output the code for w.
        if (!w.equals(""))
            result.add(dictionary.get(w));
        
        List<String> binStrings = new ArrayList<String>();
        bits = 9;
        for(int x = 0; x < result.size(); x++){
    		binStrings.add(integerToBinaryString(result.get(x), bits));
        	if(result.get(x) == (Math.pow(2, bits) - 1))
        		bits++;
        }
        
        StringBuilder sb = new StringBuilder();
        for (String s : binStrings)
        	sb.append(s);
       
        //Get the length of the binary string for decompression
        int initialLength = sb.length();
        while(sb.length() % 8 != 0)
        	sb.append('0');
        byte offset = (byte)(initialLength - sb.length());
        
        byte[] binData = new byte[sb.length() / 8];
        int count = 0;
        int count2 = 8;
    	String str = sb.toString();
        while(count2 <= str.length()){
        	String str2 = str.substring(count2 - 8, count2);
        	Integer i = Integer.valueOf(Integer.parseInt(str2, 2));
        	byte s = i.byteValue();
        	binData[count] = s;
        	count++;
        	count2 += 8;
        } 
       
        DataOutputStream dos2 = null;
        OutputStream os2 = null;
        try {
			os2 = new FileOutputStream(fileName + ".lzw2");
			dos2 = new DataOutputStream(os2);
			dos2.writeByte(offset);
			dos2.write(binData);
			dos2.flush();
			dos2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        System.out.println("File has been compressed");
    }
 
    /** Decompress a file of type LZW */
    public static void decompress(String p, String type) {
    	System.out.println("Starting decompression...");
    	ArrayList<Integer> compressed = new ArrayList<Integer>();
    	String fileName = p.substring(0, p.indexOf('.'));
    	String fileType = "." + type;
    	
    	byte offset = 0;
        DataInputStream dis2 = null;
        InputStream is2 = null;
        StringBuilder sb2 = new StringBuilder();
        List<Integer> compressedInts = new ArrayList<Integer>();
        try {
			is2 = new FileInputStream(p);
			dis2 = new DataInputStream(is2);
			offset = (byte) Math.abs(dis2.readByte());
			
			boolean EOF = false;
			while(!EOF){
				try{
					byte b = dis2.readByte();
					compressedInts.add((int) b);
					sb2.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
				} catch(EOFException e){
					EOF = true;
				}
			}
			dis2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        if(offset > 0)
        	sb2.setLength(sb2.length() - offset);
        
        String str = sb2.toString();
    	
        int count = 9;
        int bits = 9;
        while(count <= str.length()){
        	String str2 = str.substring(count - bits, count);
        	Integer i = Integer.valueOf(Integer.parseInt(str2, 2));
        	if(i == (Math.pow(2, bits) - 1) && bits != 16) {
        		bits++;
        	}
        	else
        		compressed.add(i);
           
        	count += bits;
        }
        
    	//Build the dictionary.
        int dictSize = 256;
        Map<Integer,String> dictionary = new HashMap<Integer,String>();
        for (int i = 0; i < 256; i++)
            dictionary.put(i, "" + (char)i);
 
        //Open a file for decompression
        File newFile = new File(fileName + "2M" + fileType);
    	FileWriter fw = null;
    	BufferedWriter bw = null;
    	DataOutputStream dos = null;
    	OutputStream os = null;
    	try {
    		if (!newFile.exists()){
    			newFile.createNewFile();
    		}
    		fw = new FileWriter(newFile);
    		bw = new BufferedWriter(fw);
    		os = new FileOutputStream(newFile);
    		dos = new DataOutputStream(os);
		} catch (IOException e) {
			e.printStackTrace();
		}
 
    	String w = "" + dictionary.get(compressed.remove(0));
        StringBuffer result = new StringBuffer(w);
        for (int k : compressed) {
            String entry;
            if (dictionary.containsKey(k))
                entry = dictionary.get(k);
            else if (k == dictSize)
                entry = w + w.charAt(0);
            else
                throw new IllegalArgumentException("Bad compressed k: " + k);
            
            result.append(entry);
            // Add w+entry[0] to the dictionary
            dictionary.put(dictSize++, w + entry.charAt(0));
            w = entry;
        }
        try {
        	//If the file is an image file
    		if(fileType.compareToIgnoreCase(".gif") == 0 || fileType.compareToIgnoreCase(".tiff") == 0
    				|| fileType.compareToIgnoreCase(".docx") == 0) {
        		dos.write(Base64.getDecoder().decode(result.toString()));
        	}
        	else {
        		bw.write(result.toString());
        	}
        	dos.flush();
			dos.close();
			bw.flush();
        	bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        System.out.println("File has been decompressed");
    }
    
    /** Method to convert an integer to a binary string representation of that integer **/
    private static String integerToBinaryString(Integer i, int bits) {
    	String result = Integer.toBinaryString(i);
    	int size = result.length();
    	int count = 0;
    	while(count < (bits - size)){
    		result = "0" + result;
    		count++;
    	}
    	return result;
    }
    
    
    public static void main(String[] args) {
    	String one = "";
    	String two = "";
    	String three = "";
    	 try {
    		 one = args[0];
    	     two = args[1];
    	 }
    	 catch (ArrayIndexOutOfBoundsException e){
    		 System.out.println("Command not found");
    	 }
    	 if(one.compareToIgnoreCase("e") == 0) {
    	     three = args[2];
    		 decompress(two, three);
    	} 
    	else if(one.compareToIgnoreCase("c") == 0) {
    		 compress(two);
    	}
    	else {
    		System.out.println("Arguments are invalid...");
    		System.out.println("Args: " + one + ", " + two + ", " + three);
    	} 
    } 
    
}