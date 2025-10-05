using Guna.UI2.WinForms;
using Microsoft.SqlServer.Server;
using ResturantManagement.Reports;
using ResturantManagement.View;
using System;
using System.Collections;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.SqlClient;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using System.Web.Security;
using System.Windows.Forms;
using System.Data.SqlServerCe;
using System.Security.Policy;
using System.Web.Services.Description;

namespace ResturantManagement.Model
{
    public partial class frmCheckout : Form
    {
        public frmCheckout(string tableName)
        {
            InitializeComponent();
            double.TryParse(txtBillAmount.Text, out amt);
            double.TryParse(txtRecieve.Text, out receipt);
            // for set defaul status table
            if (btnSave.Checked = true && receipt >= amt )
            {
                frmPOS frmPOS = new frmPOS();
                frmPOS.lblTable.Text = tableName;
                frmPOS.btnDin.Checked = false;
                ClassConnection.con.Open();
                string upate = "update tables set status=@status, tname=@tname where tname = '" + frmPOS.lblTable.Text + "'";
                SqlCeCommand cmd2 = new SqlCeCommand(upate, ClassConnection.con);
                cmd2.Parameters.AddWithValue("@tname", frmPOS.lblTable.Text);
                cmd2.Parameters.AddWithValue("@status", "ទំនេរ");
                cmd2.ExecuteNonQuery();
                ClassConnection.con.Close();
            } 
        }
        public static void updateTable(string tableName)
        {
            frmPOS frmPOS = new frmPOS();
            frmPOS.lblTable.Text = tableName;
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
        public void showToast(string type, string message)
        {
            ToasForm toas = new ToasForm(type, message);
            toas.Show();
        }

        public double amt;
        public double totKh;
        public int MainID = 0;
        public double disc = 0;
        public double netAmt = 0;
        public double netKh = 0;
        public double receipt = 0;
        public double change = 0;
        public double currency = 0;

        // btn discount
        private void txtDisc_TextChanged(object sender, EventArgs e)
        {
            double.TryParse(txtBillAmount.Text, out amt);
            double.TryParse(txtDisc.Text, out disc);
            double.TryParse(txtNedAmt.Text, out netAmt);
            double.TryParse(txtCurrency.Text, out netKh);

            netAmt = Math.Abs(amt - (amt * disc / 100));
            netKh = Math.Abs(netAmt * netKh);

            if (disc >= 100)
            {
                txtDisc.Text = "";
                //this.Alert("can not discount 100%", AlertMessage.enmType.Warning);
                showToast("WARNING", "can not discount 100%");
            }
            else if (disc >= 50)
            {
                txtDisc.Text = "";
                //this.Alert("can not discount 50%", AlertMessage.enmType.Warning);
                showToast("WARNING", "can not discount 50%");
            }
            txtNedAmt.Text = netAmt.ToString("F");
            txtNetKh.Text = netKh.ToString();
        }


        // btn Cash Recieved
        private void txtReceived_TextChanged(object sender, EventArgs e)
        {
            double.TryParse(txtNedAmt.Text, out netAmt);
            double.TryParse(txtRecieve.Text, out receipt);
            double.TryParse(txtChange.Text, out change);
            double.TryParse(txtChangeKH.Text, out totKh);
            double.TryParse(txtCurrency.Text,out currency);
            double.TryParse(txtCurrency.Text, out netKh);
            // if no discount
            string dic = txtDisc.Text;

            netKh = Math.Abs(netAmt * netKh); //for net amount kh
            txtNetKh.Text = netKh.ToString();  //for net amount kh

            if (dic == "")
            {
                change = Math.Abs(amt - receipt); // conver postive or negative to always positive
                totKh = Math.Abs(change * currency);     // totKh = (amt * currency from db);
                

                txtNedAmt.Text = amt.ToString();
                txtChange.Text = change.ToString();
                txtChangeKH.Text = totKh.ToString();
            }
            // if discount
            else
            {
                change = Math.Abs(netAmt - receipt); // conver postive or negative to always positive
                totKh = Math.Abs(change * currency);

                txtChange.Text = change.ToString();
                txtChangeKH.Text = totKh.ToString();
            }
           
        }
        public void updateStatustable()
        {

            // for set defaul status table
            frmPOS frmPOS = new frmPOS();
            frmCheckout frm = new frmCheckout(frmPOS.lblTable.Text);
            MessageBox.Show(frmPOS.lblTable.Text);
            ClassConnection.con.Open();
            string upate = "update tables set status=@status, tname=@tname where tname = '" + frmPOS.lblTable.Text + "'";
            SqlCeCommand cmd2 = new SqlCeCommand(upate, ClassConnection.con);
            cmd2.Parameters.AddWithValue("@tname", frmPOS.lblTable.Text);
            cmd2.Parameters.AddWithValue("@status", "ទំនេរ");
            cmd2.ExecuteNonQuery();
            ClassConnection.con.Close();
        }
        // btn save
        private void btnSave_Click(object sender, EventArgs e)
        {
            
            double.TryParse(txtBillAmount.Text, out amt);
            double.TryParse(txtRecieve.Text, out receipt);
            if(txtRecieve.Text == "")
            {
                //this.Alert("Please enter Recieve.", AlertMessage.enmType.Error);
                showToast("ERROR", "Please enter Recieve.");
            }
            else if (amt > receipt)
            {
                //this.Alert("Recieve must to be creater than \n Bill Amount.", AlertMessage.enmType.Error);
                showToast("ERROR", "Recieve must to be creater than Bill Amount");
                txtRecieve.Text = "";
                txtNedAmt.Text = "";
                txtChange.Text = "";
                txtChangeKH.Text = "";
                txtDisc.Text = "";
            }
            else
            {
                frmPOS frm1 = new frmPOS();
                frm1.btnDin.Checked = false;

                string dic = txtDisc.Text;
                string qry = @"update tblMain set total=@total,netAmt=@netAmt, totalKH=@netKh,changeKh=@changeKh,discount=@discount, received = @rec, change = @change ,
                              status = 'Paid' where MainID = @id";
                
                Hashtable ht = new Hashtable();
                ht.Add("@id", MainID);
                ht.Add("@total", txtBillAmount.Text);
                ht.Add("@netAmt", txtNedAmt.Text);
                ht.Add("@changeKh", txtChangeKH.Text);
                if (dic == "")
                {
                    ht.Add("@discount", Convert.ToDouble(0));
                }
                else
                {
                    ht.Add("@discount", txtDisc.Text);
                }
                ht.Add("@rec", txtRecieve.Text);
                ht.Add("@change", txtChange.Text);
                ht.Add("@netKh", txtNetKh.Text);

                if (ClassConnection.SQl(qry, ht) > 0)
                {
                    //this.Alert("Saved Successfully.", AlertMessage.enmType.Success);
                    showToast("SUCCESS", "Saved Successfully.");
                    this.Close();
                }


                // Print Bill
                //MainID = Convert.ToInt32(guna2DataGridView1.CurrentRow.Cells["dgvid"].Value);
                /* string qry1 = @"select * from tblMain m inner join
                             tblDetails d on d.MainID = m.MainID
                             inner join products p on p.pID = d.proID
                             where m.MainID = " + MainID + "";
                 SqlCeCommand cmd = new SqlCeCommand(qry1, ClassConnection.con);
                 ClassConnection.con.Open();
                 DataTable dt = new DataTable();
                 SqlCeDataAdapter da = new SqlCeDataAdapter(cmd);
                 SqlCeDataReader dr;
                 da.Fill(dt);
                 dr = cmd.ExecuteReader();
                 ClassConnection.con.Close();
                 frmPrint frm = new frmPrint();
                 rptBill rpt = new rptBill();

                //rpt.SetDatabaseLogon("sa", "sql123");
                 rpt.SetDataSource(dt);
                 frm.crystalReportViewer1.ReportSource = rpt;
                 frm.crystalReportViewer1.Refresh();
                 frm.Show();*/

                ClassConnection.con.Open();
                string qry1 = @"select * from tblMain m inner join
                            tblDetails d on d.MainID = m.MainID
                            inner join products p on p.pID = d.proID
                            where m.MainID = " + MainID + "";
                SqlCeCommand cmd = new SqlCeCommand(qry1, ClassConnection.con);
                SqlCeDataAdapter da = new SqlCeDataAdapter(cmd);
                System.Data.DataTable dt = new System.Data.DataTable();
                da.Fill(dt);
                frmPrint frm = new frmPrint();
                Invoice cr = new Invoice();

                cr.SetDataSource(dt);
                frm.crystalReportViewer1.ReportSource = cr;
                frm.crystalReportViewer1.Refresh();
                frm.Show();
                ClassConnection.con.Close();
            }
        }
        private void frmCheckout_Load(object sender, EventArgs e)
        {
            txtBillAmount.Text = amt.ToString("0.00");
            txtChangeKH.Text = totKh.ToString();
            txtNedAmt.Text = amt.ToString("0.00");

            loadCurr(); // 
        }

        // function load currency
        private void loadCurr()
        {
            string qry = "select * from tblCurrency";
            SqlCeDataReader dr;
            SqlCeCommand cmd = new SqlCeCommand(qry, ClassConnection.con);
            ClassConnection.con.Open();
            dr = cmd.ExecuteReader();
            //dr.Read();
            if (dr.Read())
            {
                txtCurrency.Text = dr[1].ToString();
            }
            dr.Close();
            ClassConnection.con.Close();
        }
        private void btnClose_Click(object sender, EventArgs e)
        {
            this.Close();
        }

        // validate input number only
        private void txtDisc_KeyPress(object sender, KeyPressEventArgs e)
        {
            e.Handled = !char.IsDigit(e.KeyChar) && !char.IsControl(e.KeyChar) && e.KeyChar != '.' && e.KeyChar !=',';           
        }
        private void txtRecieve_KeyPress(object sender, KeyPressEventArgs e)
        {
            e.Handled = !char.IsDigit(e.KeyChar) && !char.IsControl(e.KeyChar) && e.KeyChar != '.' && e.KeyChar != ',';
        }
    }
}
