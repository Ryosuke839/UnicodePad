/*
   Copyright 2018 Ryosuke839

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package jp.ddo.hotmist.unicodepad;

import java.util.ArrayList;

class StringAdapter extends UnicodeAdapter
{
	private ArrayList<Integer> list;

	StringAdapter(String str)
	{
		super(null, null, false);

		list = new ArrayList<>();
		for (int i = 0; i < str.length(); ++i)
		{
			int code = str.codePointAt(i);
			if (code > 0xFFFF)
				++i;
			list.add(code);
		}
	}

	@Override
	public int getCount()
	{
		return list.size();
	}

	@Override
	public long getItemId(int arg0)
	{
		return list.get(arg0);
	}
}
