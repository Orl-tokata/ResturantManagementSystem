using Guna.UI2.WinForms;
using ResturantManagement.FormLogin;
using ResturantManagement.Model;
using ResturantManagement.Reports;
//using ResturantManagement.UserAccount;
using ResturantManagement.View;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.SqlClient;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Web.UI.WebControls;
using System.Windows.Forms;
using System.Xml.Linq;
using System.Data.SqlServerCe;
using Microsoft.Office.Interop.Excel;
using System.IO;

namespace ResturantManagement
{
    public partial class frmMain : Form
    {
        SqlCeDataReader dr;
        public frmMain()
        {
            InitializeComponent();
            btnHome.Checked = true;
        }
        // declear get user name and role
        public string _username = "", _name = "", _role = "";
        bool _found = true;

        // get for accessing frmMain
        static frmMain _obj;
        public string _pass;
        public static frmMain Instance
        {
            get { if(_obj == null) { _obj = new frmMain(); } return _obj; }
        }
        public static void GetUser(string role)
        {
            SqlCeDataReader dr;
            string _role = "";

            bool found;
            ClassConnection.con.Open();
            string qry = "select * from users "; 
            SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
            dr = cm.ExecuteReader();
            //dr.Read();
            if (dr.Read())
            {
                found = true;
                _role = dr["uRole"].ToString();
            }
            else
            {
                found = false;
            }
            dr.Close();
            ClassConnection.con.Close();

            if (found)
            {
                if (_role == "Cashier")
                {
                    frmMain main = new frmMain();
                    main.ShowDialog();
                }
            }
        }

        // Method to add Controls in Main Form
        public void AddControls(Form f)
        {
            CenterPanel.Controls.Clear();
            f.Dock = DockStyle.Fill;
            f.TopLevel = false;
            CenterPanel.Controls.Add(f);
            f.Show();
        }

        private void btnExit_Click(object sender, EventArgs e)
        {
            System.Windows.Forms.Application.Exit();
        }

        private void btnLogout_Click(object sender, EventArgs e)
        {
            guna2MessageDialog1.Icon = Guna.UI2.WinForms.MessageDialogIcon.Question;
            guna2MessageDialog1.Buttons = Guna.UI2.WinForms.MessageDialogButtons.YesNo;
            
            if (guna2MessageDialog1.Show("Logout Application?") == DialogResult.Yes)
            {
                this.Hide();
                frmLogin login = new frmLogin();
                login = new frmLogin();
                login = new frmLogin();
                login.ShowDialog();
            }
        }

        private void frmMain_Load(object sender, EventArgs e)
        {
            _obj = this;
            // for show form dashboard first
            AddControls(new frmDashboard());
        }

        private void btnHome_Click(object sender, EventArgs e)
        {
            AddControls(new frmDashboard());
        }

        private void btnCategory_Click(object sender, EventArgs e)
        {
            AddControls(new frmCategoryView());
        }

        private void btnTable_Click(object sender, EventArgs e)
        {
            AddControls(new frmTableView());
        }

        private void btnStaff_Click(object sender, EventArgs e)
        {
            AddControls(new frmUserStaffInfo());
        }

        private void btnProduct_Click(object sender, EventArgs e)
        {
            AddControls(new frmProductView());
        }
        private void btnPOS_Click(object sender, EventArgs e)
        {
            frmPOS frm = new frmPOS();
            frm.ShowDialog();

        }

        private void btnKitchen_Click(object sender, EventArgs e)
        {
            AddControls(new frmKitchenView());
        }

        private void btnReport_Click(object sender, EventArgs e)
        {
            AddControls(new frmDailyReport());
        }

        private void btnChangePass_Click(object sender, EventArgs e)
        {
            FormLogin.ChangePassword change = new FormLogin.ChangePassword();
            MainClass.BlueBackground(change);
        }
        // btn help 
        private void help_Click_1(object sender, EventArgs e)
        {
            frmHelp help = new frmHelp();
            MainClass.BlueBackground(help);
        }
        // btn about us
        private void aboutUs_Click_1(object sender, EventArgs e)
        {
            frmAboutUs aboutus = new frmAboutUs();
            MainClass.BlueBackground(aboutus);
        }
        // btn currency
        private void lblCurrency_Click(object sender, EventArgs e)
        {
            CurrencyDaily currency = new CurrencyDaily();
            MainClass.BlueBackground(currency);
        }
        // btn help 
        private void toolStripMenuItem2_Click(object sender, EventArgs e)
        {
            frmHelp help = new frmHelp();
            MainClass.BlueBackground(help);
        }
        // btn about us
        private void toolStripMenuItem3_Click(object sender, EventArgs e)
        {
            frmAboutUs aboutus = new frmAboutUs();
            MainClass.BlueBackground(aboutus);
        }

        private void btnAttendance_Click(object sender, EventArgs e)
        {
            AddControls(new frmAttendanceCashier());
        }
        private void timer1_Tick(object sender, EventArgs e)
        {
            lblTime.Text = DateTime.Now.ToString("MMMM dd, yyyy HH:ss:tt");
        }
    }
}
