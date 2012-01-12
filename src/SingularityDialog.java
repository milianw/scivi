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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import jv.vecmath.PdVector;

@SuppressWarnings("serial")
public class SingularityDialog extends JDialog implements ActionListener
{
	private PdVector m_pos;
	private Term m_term;
	private JButton m_okButton;
	private JButton m_cancelButton;

	public SingularityDialog(Frame owner, PdVector point)
	{
		super(owner);
		m_pos = point;

		Panel panel = new Panel();
		panel.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 0;

		Font boldFont = new Font("Dialog", Font.BOLD, 12);

		c.fill = GridBagConstraints.CENTER;
		Label l = new Label("Add Singularity at: " + m_pos.toShortString());
		l.setFont(boldFont);
		panel.add(l, c);
		c.fill = GridBagConstraints.HORIZONTAL;

		getContentPane().add(panel);

		JPanel buttonPane = new JPanel();

		m_okButton = new JButton("Apply");
		m_okButton.addActionListener(this);
		buttonPane.add(m_okButton);

		m_cancelButton = new JButton("Cancel");
		m_cancelButton.addActionListener(this);
		buttonPane.add(m_cancelButton);

		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
	}
	public Term getSingularity()
	{
		return m_term;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == m_okButton) {
			m_term = new SinkTerm(m_pos);
			setVisible(false);
		} else if (source == m_cancelButton) {
			setVisible(false);
		}
	}
}