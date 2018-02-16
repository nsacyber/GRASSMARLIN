using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ImageToData
{
    public partial class Form1 : Form
    {
        public Form1()
        {
            InitializeComponent();
        }

        private void button1_Click(object sender, EventArgs e)
        {
            if(openFileDialog1.ShowDialog() == DialogResult.OK)
            {
                StringBuilder sbResult = new StringBuilder();                
                sbResult.Append("data:image/png;base64,");
                System.IO.Stream streamContents = openFileDialog1.OpenFile();
                byte[] contents = new byte[streamContents.Length];
                streamContents.Seek(0, System.IO.SeekOrigin.Begin);
                streamContents.Read(contents, 0, contents.Length);
                streamContents.Close();
                sbResult.Append(System.Convert.ToBase64String(contents));
                Clipboard.SetText(sbResult.ToString());
            }
        }
    }
}
