using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ResturantManagement.View
{
    public partial class frmHelp : Form
    {
        public frmHelp()
        {
            InitializeComponent();
        }

        private void timer1_Tick(object sender, EventArgs e)
        {
            if (this.Opacity > 0.0)
            {
                this.Opacity -= 0.75;
            }
            else
            {
                timer1.Stop();
                this.Close();
            }
        }

        private void btnClose_Click(object sender, EventArgs e)
        {
            timer1.Start();
        }
   
        private void frmHelp_Load(object sender, EventArgs e)
        {
            // for text animation
            txts = lblTxt.Text;
            mimic = txts.Length;
            lblTxt.Text = "";
            //loading the flying label
        }
        private int counter = 0, mimic = 0;
        private string txts;
        private void timer3_Tick(object sender, EventArgs e)
        {
            counter++;
            if (counter > mimic)
            {
                counter = 0;
                lblTxt.Text = "";
            }
            else
            {
                lblTxt.Text = txts.Substring(0, counter);
                if (lblTxt.ForeColor == Color.Yellow)
                    lblTxt.ForeColor = Color.Red;
                else
                    lblTxt.ForeColor = Color.Yellow;
            }
        }
    }
}
