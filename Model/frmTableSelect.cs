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
using System.Data.SqlServerCe;

namespace ResturantManagement.Model
{
    public partial class frmTableSelect : Form
    {
        public frmTableSelect()
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
        public string TableName;

        private void frmTableSelect_Load(object sender, EventArgs e)
        {
            string qry = "select tid, tname, status from tables";
            SqlCeCommand cmd = new SqlCeCommand(qry, ClassConnection.con);
            SqlCeDataAdapter da = new SqlCeDataAdapter(cmd);
            DataTable dt = new DataTable();
            da.Fill(dt);
           
            foreach (DataRow row in dt.Rows)
            {
                Guna.UI2.WinForms.Guna2Button b = new Guna.UI2.WinForms.Guna2Button();
                b.Text = row["tname"].ToString();
                b.Width = 150;
                b.Height = 50;
                b.FillColor = Color.FromArgb(255, 139, 19);
                b.HoverState.FillColor = Color.FromArgb(50, 55, 89);
                b.Font = new Font("Khmer OS Battambang", 14);
                b.BorderRadius = 10;
                // event for click
                b.Click += new EventHandler(b_click);
                flowLayoutPanel1.Controls.Add(b);

                //event check status button
                if (row["status"].ToString() == "មិនទំនេរ")
                {
                    //b.Width = 175;
                    //b.Text = row["tname"].ToString() + " មិនទំនេរ";
                    b.FillColor = Color.FromArgb(123, 143, 161);
                    b.Enabled = false;
                }
                else if (row["status"].ToString() == "ទំនេរ")
                {
                    b.FillColor = Color.FromArgb(255, 139, 19);
                    b.Enabled = true;
                }
            }
        }

        private void b_click(object sender, EventArgs e)
        {
            TableName = (sender as Guna.UI2.WinForms.Guna2Button).Text.ToString();

            /*ClassConnection.con.Open();
            string upate = "update tables set status=@status, tname=@tname where tname = '" + TableName + "'";
            SqlCeCommand cmd1 = new SqlCeCommand(upate, ClassConnection.con);
            cmd1.Parameters.AddWithValue("@tname", TableName);
            cmd1.Parameters.AddWithValue("@status", "មិនទំនេរ");
            cmd1.ExecuteNonQuery();

            MessageBox.Show(TableName);

            if (ClassConnection.con.State == ConnectionState.Closed) { ClassConnection.con.Open(); }
            if (ClassConnection.con.State == ConnectionState.Open) { ClassConnection.con.Close(); }*/

            this.Close();
        }
        protected override void OnDeactivate(EventArgs e)
        {
            base.OnDeactivate(e);
            this.Close();
        }

        private void frmTableSelect_Deactivate(object sender, EventArgs e)
        {
            this.Close();
        }
    }
}
