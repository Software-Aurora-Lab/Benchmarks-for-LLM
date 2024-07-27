package stringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import unsafeString.DirectCharArray;

public class StringUtils {

	
	public static String[] split(String txt, char c) {
		
		String txt2 = txt;
		List<String> list = new ArrayList<>();
		
		int pos = 0;
		while((pos = txt2.indexOf(',')) > 0) {
		
			list.add(txt2.substring(0, pos));
			txt2 = txt2.substring(pos + 1);
		}
		if(txt2 != null && txt2.length() > 0) {
			list.add(txt2);
		}
		
		int resultSize = list.size();
		
        String[] result = new String[resultSize];
        return list.toArray(result);
	}

	public static String[] unsafeCopy(String str, char c) {
		
		str = str + c;
		
		DirectCharArray direct = DirectCharArray.getInstance();
		direct.allocate(str.length());
		
		direct.intern(str);
		
		List<String> col = new ArrayList<>();

		char[] word = new char[1000];
		int cnt = 0;

		for(int i = 0; i < str.length(); i++) {
			char cur = direct.get(i);

			if(cur == c) {
				col.add( new String(Arrays.copyOfRange(word, 0, cnt)));
				cnt = 0;
				continue;
			}
			word[cnt] = cur;
			cnt++;
		}

		return col.toArray(new String[col.size()]);
	}
	
	public static String[] unsafecountBefore(String str, char c) {
		
		str = str + c;
		
		DirectCharArray direct = DirectCharArray.getInstance();
		direct.allocate(str.length());
		
		List idxs = new ArrayList();
		long cnt = 0L;
		for(char cur : str.toCharArray()) {
			
			direct.set(cnt, cur);
			
			if(cur == c) {
				
				idxs.add(cnt);
				
			}
			cnt++;
		}
		
		String[] column = new String[idxs.size()];

		long start = 0L;
		long stop = 0L;
		for(int i = 0; i < idxs.size(); i++) {
			stop  = (Long) idxs.get(i) + 1;
			column[i] = new String(direct.get(start, stop));
			start = stop;
		}
		
		return column;
	}
	
	public static String[] loop(String str, char c) {
		
		str = str + c;

		int lenght = 0;
		int start = 0;
		
		List<String> col = new ArrayList<>();
		
		for(int i = 0; i < str.length(); i++) {
			char cur = str.charAt(i);

			if(cur == c) {
				col.add(str.substring(start, start + lenght));
				start = i + 1;
				lenght = 0;
				continue;
			}
			lenght++;
		}
		
		return col.toArray(new String[col.size()]);
	}
	
	
	
}
