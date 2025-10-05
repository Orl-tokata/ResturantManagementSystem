using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.SqlClient;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using static MetroFramework.Controls.MetroTextBox;
using System.Data.SqlServerCe;

namespace ResturantManagement.Model
{
    public partial class frmWaiterSelect : Form
    {
        public frmWaiterSelect()
        {
            InitializeComponent();
        }
        // this Enable double buffering for all the controls
        protected override CreateParams CreateParams
        {
            get
            {
                CreateParams handleParam = base.CreateParams;
                handleParam.ExStyle |= 0x02000000;
                return handleParam;
            }
        }
        public string waiterName;
        private void frmWaiterSelect_Load(object sender, EventArgs e)
        {
            string qry = "select * from staff where sRole like 'Waiter'";
            SqlCeCommand cmd = new SqlCeCommand(qry, ClassConnection.con);
            SqlCeDataAdapter da = new SqlCeDataAdapter(cmd);
            DataTable dt = new DataTable();
            da.Fill(dt);

            foreach (DataRow row in dt.Rows)
            {
                Guna.UI2.WinForms.Guna2Button b = new Guna.UI2.WinForms.Guna2Button();
                b.Text = row["sName"].ToString();
                b.Width = 150;
                b.Height = 50;
                b.FillColor = Color.FromArgb(255, 139, 19);
                b.HoverState.FillColor = Color.FromArgb(50, 55, 89);
                b.Font = new Font("Khmer OS Battambang", 14);
                b.BorderRadius = 10;
                // event for click
                b.Click += new EventHandler(b_click);
                flowLayoutPanel1.Controls.Add(b);
            }
        }

        private void b_click(object sender, EventArgs e)
        {
            waiterName = (sender as Guna.UI2.WinForms.Guna2Button).Text.ToString();
            this.Close();
        }
    }
}
