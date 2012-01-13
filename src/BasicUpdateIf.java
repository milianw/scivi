/*
	Copyright 2012 Milian Wolff <mail@milianw.de>
	
	This program is free software; you can redistribute it and/or
	modify it under the terms of the GNU General Public License as
	published by the Free Software Foundation; either version 2 of 
	the License, or (at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import jv.object.PsUpdateIf;

class BasicUpdateIf implements PsUpdateIf
{
	private PsUpdateIf m_father;
	@Override
	public PsUpdateIf getFather() {
		return m_father;
	}

	@Override
	public String getName() {
		return getClass().getName();
	}

	@Override
	public void setParent(PsUpdateIf parent) {
		m_father = parent;
	}

	@Override
	public boolean update(Object event) {
		if (m_father != null) {
			return m_father.update(event);
		}
		return false;
	}
	
}