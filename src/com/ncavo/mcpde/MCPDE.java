package com.ncavo.mcpde;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MCPDE {
	public static short readShort(InputStream r) throws IOException {
		byte buf[] = new byte[2];
		r.read(buf);
		short a = 0;
		a |= (int)buf[0] & 0xFF;
		a <<= 8;
		a |= (int)buf[1] & 0xFF;
		return a;
	}

	public static void writeShort(OutputStream w, short i) throws IOException {
		byte buf[] = new byte[2];
		buf[1] = (byte)(0xFF & i);
		i >>= 8;
		buf[0] = (byte)(0xFF & i);
		w.write(buf);
	}

	public static int readInt(InputStream r) throws IOException {
		byte buf[] = new byte[4];
		r.read(buf);
		int a = 0;
		for(int i = 0; i < 4; i++) {
			a <<= 8;
			a |= (int)buf[i] & 0xFF;
		}
		return a;
	}

	public static void writeInt(OutputStream w, int i) throws IOException {
		byte buf[] = new byte[4];
		buf[3] = (byte)(0xFF & i);
		i >>= 8;
		buf[2] = (byte)(0xFF & i);
		i >>= 8;
		buf[1] = (byte)(0xFF & i);
		i >>= 8;
		buf[0] = (byte)(0xFF & i);
		w.write(buf);
	}
	
	public static int readWriteInt(InputStream r, OutputStream w) throws IOException {
		byte buf[] = new byte[4];
		r.read(buf);
		w.write(buf);
		int a = 0;
		for(int i = 0; i < 4; i++) {
			a <<= 8;
			a |= (int)buf[i] & 0xFF;
		}
		return a;
	}

	public static long readWriteLong(InputStream r, OutputStream w) throws IOException {
		byte buf[] = new byte[8];
		r.read(buf);
		w.write(buf);
		long a = 0;
		for(int i = 0; i < 8; i++) {
			a <<= 8;
			a |= (int)buf[i] & 0xFF;
		}
		return a;
	}
	
	public static float readFloat(InputStream r) throws IOException {
		byte buf[] = new byte[4];
		r.read(buf);
		int a = 0;
		for(int i = 0; i < 4; i++) {
			a <<= 8;
			a |= (int)buf[i] & 0xFF;
		}
		return Float.intBitsToFloat(a);
	}

	public static void writeFloat(OutputStream w, float f) throws IOException {
		byte buf[] = new byte[4];
		long l = Float.floatToRawIntBits(f);
		for(int i = 4 - 1; i >= 0; i--) {		
			buf[i] = (byte)(0xFF & l);
			l >>= 8;
		}
		w.write(buf);
	}

	public static double readDouble(InputStream r) throws IOException {
		byte buf[] = new byte[8];
		r.read(buf);
		long a = 0;
		for(int i = 0; i < 8; i++) {
			a <<= 8;
			a |= (long)buf[i] & 0xFF;
		}
		return Double.longBitsToDouble(a);
	}

	public static void writeDouble(OutputStream w, double d) throws IOException {
		byte buf[] = new byte[8];
		long l = Double.doubleToRawLongBits(d);
		for(int i = 8 - 1; i >= 0; i--) {		
			buf[i] = (byte)(0xFF & l);
			l >>= 8;
		}
		w.write(buf);
	}
		
	public static String readWriteStr(InputStream r, OutputStream w) throws IOException {
		byte buf[] = new byte[2];
		r.read(buf);
		w.write(buf);
		int a = 0;
		a |= (int)buf[0] & 0xFF;
		a <<= 8;
		a |= (int)buf[1] & 0xFF;
		buf = new byte[a];
		r.read(buf);
		w.write(buf);
		return new String(buf);
	}
	
	public static void main(String[] args) {
		if(args.length == 0) {
			System.out.println("Usage: MCPDE {playerdata file name}");
			return;
		}
		String outFileName = args[0] + ".edit";
		try (BufferedInputStream br = new BufferedInputStream(new GZIPInputStream(new FileInputStream(args[0])));
			BufferedOutputStream bw = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(outFileName, false)))) {
			bw.write(br.readNBytes(3));
			int status = 0;
			int depth = 0;
			String listName[] = new String[16];
			int listType[] = new int[16];
			int listSize[] = new int[16];
			int listIdx[] = new int[16];
			while(true) {
				int c = br.read();
				if(c == -1) {
					if(depth > 0) {
						System.out.println("unexpected end");
						status = -1;
					}
					else {
						System.out.println("end");						
						status = 1;
					}
					break;					
				}
				bw.write(c);
				switch(c) {
				case 0: // end
					if(depth == 0) {
						System.out.println("end");						
						status = 1;
						break;
					}
					depth--;
					if(listType[depth] != -1) {
						System.out.println("unexpected 0");					
						status = -1;
						break;
					}
					if(depth > 0 && listType[depth - 1] != -1) {
						depth--;
						if(++listIdx[depth] < listSize[depth]) {
							depth++;
							listName[depth] = "Compo of list #" + (listIdx[depth - 1] + 1);
							System.out.println("\t".repeat(depth) + listName[depth]);
							listType[depth] = -1;
							depth++;
						}						
					}
					break;
				case 1: // byte
				{
					String name = readWriteStr(br, bw);
					int value = br.read();
					if(depth > 1 && listName[depth - 2].equalsIgnoreCase("Inventory")) {
						if(name.equalsIgnoreCase("Count") && value == 2) {
							value = 64;
						}
					}						
					bw.write(value);
					System.out.println("\t".repeat(depth) + "Byte:" + name + "=" + value);
				}
				break;
				case 2: // short
				{
					String name = readWriteStr(br, bw);
					short value = readShort(br);
					writeShort(bw, value);
					System.out.println("\t".repeat(depth) + "Short:" + name + "=" + value);
				}
				break;
				case 3: // int
				{
					String name = readWriteStr(br, bw);
					int value = readInt(br);
					if(depth > 2 && listName[depth - 3].equalsIgnoreCase("Inventory")) {
						if(name.equalsIgnoreCase("Damage")) {
							value = 0;
						}
					}
					writeInt(bw, value);
					System.out.println("\t".repeat(depth) + "Int:" + name + "=" + value);
				}
				break;
				case 4: // long
				{
					String name = readWriteStr(br, bw);
					long value = readWriteLong(br, bw);
					System.out.println("\t".repeat(depth) + "Int:" + name + "=" + value);
				}
				break;
				case 5: // float
				{
					String name = readWriteStr(br, bw);
					float value = readFloat(br);
					writeFloat(bw, value);
					System.out.println("\t".repeat(depth) + "Float:" + name + "=" + value);
				}
				break;
				case 6: // double
				{
					String name = readWriteStr(br, bw);
					double value = readDouble(br);
					writeDouble(bw, value);
					System.out.println("\t".repeat(depth) + "Double:" + name + "=" + value);
				}
				break;
				case 7:
				{
					String name = readWriteStr(br, bw);
					int size = readWriteInt(br, bw);
					System.out.println("\t".repeat(depth) + "Array:" + name);
					for(int i = 0; i < size; i++) {
						int value = br.read();
						bw.write(value);
						System.out.println("\t".repeat(depth + 1) + "Byte:" + value);
					}
				}						
				break;				
				case 8: // string
				{
					String name = readWriteStr(br, bw);
					String value = readWriteStr(br, bw);
					System.out.println("\t".repeat(depth) + "Str:" + name + "=" + value);
				}	
				break;
				case 9: // list
					listName[depth] = readWriteStr(br, bw);
					System.out.println("\t".repeat(depth) + "List:" + listName[depth]);
					listType[depth] = br.read();
					bw.write(listType[depth]);
					listSize[depth] = readWriteInt(br, bw);
					listIdx[depth] = 0;
					switch(listType[depth]) {
					case 0:
						System.out.println("\t".repeat(depth + 1) + "Empty List");
						break;
					case 5:
						while(listIdx[depth] < listSize[depth]) {
							float listValue = readFloat(br);
							writeFloat(bw, listValue);
							System.out.println("\t".repeat(depth + 1) + "Float:" + listValue);
							listIdx[depth]++;
						}
						break;
					case 6:
						while(listIdx[depth] < listSize[depth]) {
							double listValue = readDouble(br);
							writeDouble(bw, listValue);
							System.out.println("\t".repeat(depth + 1) + "Double:" + listValue);
							listIdx[depth]++;
						}
						break;
					case 8:
						while(listIdx[depth] < listSize[depth]) {
							String listValue = readWriteStr(br, bw);
							System.out.println("\t".repeat(depth + 1) + "Str:" + listValue);
							listIdx[depth]++;
						}
						break;
					case 10:
						depth++;
						listName[depth] = "Compo of list #" + (listIdx[depth - 1] + 1);
						System.out.println("\t".repeat(depth) + listName[depth]);
						listType[depth] = -1;
						depth++;						
						break;
					default:
						System.out.println("unknown list type " + listType[depth]);
						status = -1;
						break;
					}
					break;
				case 10: // component
					listName[depth] = readWriteStr(br, bw);
					System.out.println("\t".repeat(depth) + "Comp:" + listName[depth]);
					listType[depth] = -1;
					depth++;
					break;
				case 11:
				{
					String name = readWriteStr(br, bw);
					int size = readWriteInt(br, bw);
					System.out.println("\t".repeat(depth) + "Array:" + name);
					for(int i = 0; i < size; i++) {
						int value = readWriteInt(br, bw);
						System.out.println("\t".repeat(depth + 1) + "Int:" + value);
					}
				}						
				break;
				case 12:
				{
					String name = readWriteStr(br, bw);
					int size = readWriteInt(br, bw);
					System.out.println("\t".repeat(depth) + "Array:" + name);
					for(int i = 0; i < size; i++) {
						long value = readWriteLong(br, bw);
						System.out.println("\t".repeat(depth + 1) + "Long:" + value);
					}
				}						
				break;
				default:
					System.out.println("\t".repeat(depth) + "not implemented type " + c);
					status = -1;
					break;
				}					
				if(status != 0) break;
			}
			br.close();
			bw.close();
			if(status == 1) {
				new File(args[0].concat(".bak")).delete();
				if(new File(args[0]).renameTo(new File(args[0].concat(".bak")))) {
					if(new File(outFileName).renameTo(new File(args[0])))
						System.out.println("done");						
					else
						System.out.println("rename err1");											
				}
				else
					System.out.println("rename err2");											
			}
			else
				new File(outFileName).delete();			
		} catch (FileNotFoundException e) {
			System.out.println("file not found: " + args[0]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
