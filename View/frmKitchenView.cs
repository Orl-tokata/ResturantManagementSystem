using ResturantManagement.Model;
using ResturantManagement.Reports;
using System;
using System.Collections;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.SqlClient;
using System.Drawing;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Data.SqlServerCe;
using CrystalDecisions.Shared.Json;
using System.Drawing.Imaging;

namespace ResturantManagement.View
{
    public partial class frmKitchenView : Form
    {
        public frmKitchenView()
        {
            InitializeComponent();
        }
        /*public void Alert(string msg, AlertMessage.enmType type)
        {
            AlertMessage alert = new AlertMessage();
            alert.showAlert(msg, type);
        }*/
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
        public string tName;
        public string inVo;
        int mid = 0;
        public void showToast(string type, string message)
        {
            ToasForm toas = new ToasForm(type, message);
            toas.Show();
        }
        private void frmKitchenView_Load(object sender, EventArgs e)
        {
            GetOrders();
        }

        private void GetOrders()
        {
            flowLayoutPanel1.Controls.Clear();
            DateTime date = DateTime.Now;
            string qry1 = @"Select * from tblMain where status = 'Pending' and aDate = '" + date.ToShortDateString() + "'";
            SqlCeCommand cmd1 = new SqlCeCommand(qry1, ClassConnection.con);
            DataTable dt1 = new DataTable();
            SqlCeDataAdapter da = new SqlCeDataAdapter(cmd1);
            da.Fill(dt1);
            FlowLayoutPanel p1;
            if (dt1.Rows.Count <= 0)
            {
                //MessageBox.Show("No data found");
                lblNotData.Show();
                flowLayoutPanel1.Hide();
            }
            else if(dt1.Rows.Count > 0)
            {
                lblNotData.Hide();
                flowLayoutPanel1.Show();
                for (int i = 0; i < dt1.Rows.Count; i++)
                {
                    p1 = new FlowLayoutPanel();
                    p1.AutoSize = true;
                    p1.Width = 230;
                    p1.Height = 350;
                    p1.FlowDirection = FlowDirection.TopDown;
                    p1.BorderStyle = BorderStyle.FixedSingle;
                    p1.Margin = new Padding(10, 10, 10, 10);

                    inVo = dt1.Rows[i]["invoiceNo"].ToString();
                    tName = dt1.Rows[i]["TableName"].ToString();

                    FlowLayoutPanel p2 = new FlowLayoutPanel();
                    p2 = new FlowLayoutPanel();
                    p2.BackColor = Color.FromArgb(50, 55, 89);
                    p2.AutoSize = true;
                    p2.Width = 230;
                    p2.Height = 125;
                    p2.FlowDirection = FlowDirection.TopDown;
                    //p2.BorderStyle = BorderStyle.FixedSingle;
                    p2.Margin = new Padding(0, 0, 0, 0);

                    Label lb = new Label();
                    lb.ForeColor = Color.White;
                    lb.Margin = new Padding(10, 10, 3, 0);
                    lb.AutoSize = true;
                    lb.Font = new Font("Segoe UI", 14);

                    Label lb1 = new Label();
                    lb1.ForeColor = Color.White;
                    lb1.Margin = new Padding(10, 10, 3, 0);
                    lb1.AutoSize = true;
                    lb1.Font = new Font("Segoe UI", 14);

                    Label lb2 = new Label();
                    lb2.ForeColor = Color.White;
                    lb2.Margin = new Padding(10, 5, 3, 0);
                    lb2.AutoSize = true;
                    lb2.Font = new Font("Segoe UI", 14);

                    Label lb3 = new Label();
                    lb3.ForeColor = Color.White;
                    lb3.Margin = new Padding(10, 5, 3, 0);
                    lb3.AutoSize = true;
                    lb3.Font = new Font("Segoe UI", 14);

                    Label lb4 = new Label();
                    lb4.ForeColor = Color.White;
                    lb4.Margin = new Padding(10, 5, 3, 10);
                    lb4.AutoSize = true;
                    lb4.Font = new Font("Segoe UI", 14);

                    
                    lb.Text = "Invoice# " + inVo;
                    lb1.Text = "Table : " + tName;
                    lb2.Text = "Waiter Name : " + dt1.Rows[i]["WaiterName"].ToString();
                    lb3.Text = "Order Time : " + dt1.Rows[i]["aTime"].ToString();
                    lb4.Text = "Order Type : " + dt1.Rows[i]["orderType"].ToString();

                    p2.Controls.Add(lb);
                    p2.Controls.Add(lb1);
                    p2.Controls.Add(lb2);
                    p2.Controls.Add(lb3);
                    p2.Controls.Add(lb4);

                    p1.Controls.Add(p2);

                    // now add products
                    
                    mid = Convert.ToInt32(dt1.Rows[i]["MainID"].ToString());

                    string qry2 = @"Select * from tblMain m
                              inner join tblDetails d on m.MainID = d.MainID
                              inner join products p on p.pID = d.proID
                                 Where m.MainID = " + mid + "";

                    SqlCeCommand cmd2 = new SqlCeCommand(qry2, ClassConnection.con);
                    DataTable dt2 = new DataTable();
                    SqlCeDataAdapter da2 = new SqlCeDataAdapter(cmd2);
                    da2.Fill(dt2);

                    for (int j = 0; j < dt2.Rows.Count; j++)
                    {
                        Label lb5 = new Label();
                        lb5.ForeColor = Color.Black;
                        lb5.Margin = new Padding(10, 5, 3, 0);
                        lb5.AutoSize = true;
                        lb5.Font = new Font("Segoe UI", 14);

                        int no = j + 1;

                        lb5.Text = "" + no + ". " + dt2.Rows[j]["pName"].ToString() + " " + dt2.Rows[j]["qty"].ToString();
                        lb5.Text = "" + no + ". " + dt2.Rows[j]["pName"].ToString() + " " + dt2.Rows[j]["qty"].ToString();
                        p1.Controls.Add(lb5);
                    }

                    // Add button to cancel  the order status
                    Guna.UI2.WinForms.Guna2Button b2 = new Guna.UI2.WinForms.Guna2Button();
                    b2.AutoRoundedCorners = true;
                    b2.Size = new Size(100, 35);
                    b2.FillColor = Color.FromArgb(192, 0, 0);
                    b2.Margin = new Padding(50, 5, 3, 5);
                    b2.Text = "Cancel";
                    b2.Font = new Font("Segoe UI", 12);
                    b2.Tag = new ButtonTag { InvoiceNo = inVo, TableName = tName };
                    b2.Click += new EventHandler(b_cancel);

                    // Add button to change  the order status
                    Guna.UI2.WinForms.Guna2Button b = new Guna.UI2.WinForms.Guna2Button();
                    b.AutoRoundedCorners = true;
                    b.Size = new Size(100, 35);
                    b.FillColor = Color.FromArgb(0, 0, 192);
                    b.Margin = new Padding(50, 5, 3, 5);
                    b.Text = "Complete";
                    b.Font = new Font("Segoe UI", 12);
                    b.Tag = dt1.Rows[i]["MainID"].ToString(); // store the id

                    b.Click += new EventHandler(b_click);

                    //p1.Controls.Add(b3);
                    p1.Controls.Add(b2);
                    p1.Controls.Add(b);

                    flowLayoutPanel1.Controls.Add(p1);
                }
            }
        }
        private void b_click(object sender, EventArgs e)
        {
            int id = Convert.ToInt32((sender as Guna.UI2.WinForms.Guna2Button).Tag.ToString());

            guna2MessageDialog1.Icon = Guna.UI2.WinForms.MessageDialogIcon.Question;
            guna2MessageDialog1.Buttons = Guna.UI2.WinForms.MessageDialogButtons.YesNo;
            if (guna2MessageDialog1.Show("Are you sure, you want to complete order?") == DialogResult.Yes)
            {
                string qry = @"update tblMain set status = 'Complete' where MainID = @ID";
                Hashtable ht = new Hashtable();
                ht.Add("@ID", id);

                if(ClassConnection.SQl(qry,ht) > 0)
                {
                    showToast("SUCCESS", "Saved Successfully.");
                }
                GetOrders();
            }
        }

        // btn cancel
        private void b_cancel(object sender, EventArgs e)
        {
            if (sender is Guna.UI2.WinForms.Guna2Button button)
            {
                // Extract information from the Tag property
                if (button.Tag is ButtonTag buttonTag)
                {
                    // Perform the update and delete operations based on the information
                    guna2MessageDialog1.Icon = Guna.UI2.WinForms.MessageDialogIcon.Question;
                    guna2MessageDialog1.Buttons = Guna.UI2.WinForms.MessageDialogButtons.YesNo;
                    if (guna2MessageDialog1.Show("Are you sour want to remove this cart?") == DialogResult.Yes)
                    {
                        UpdateTable(buttonTag.InvoiceNo, buttonTag.TableName);
                        DeleteInvoice(buttonTag.InvoiceNo, buttonTag.TableName);
                        GetOrders();
                    }
                }
            }
        }

        private void UpdateTable(string InvoiceNo, string tableName)
        {
            ClassConnection.con.Open();
            string upate = "update tables set status=@status, tname=@tname where tname=@tname";
            SqlCeCommand cm = new SqlCeCommand(upate, ClassConnection.con);
            cm.Parameters.AddWithValue("@tname", tableName);
            cm.Parameters.AddWithValue("@status", "ទំនេរ");
            cm.ExecuteNonQuery();
            ClassConnection.con.Close();
        }

        private void DeleteInvoice(string InvoiceNo, string tableName)
        {
            ClassConnection.con.Open();

            string qry = @"Delete from tblMain where invoiceNo=@invoiceNo";
            SqlCeCommand cm1 = new SqlCeCommand(qry, ClassConnection.con);
            cm1.Parameters.AddWithValue("@invoiceNo", InvoiceNo);
            cm1.ExecuteNonQuery();
            ClassConnection.con.Close();
        }

        // Define a custom class to store information in the Tag property
        private class ButtonTag
        {
            public string InvoiceNo { get; set; }
            public string TableName { get; set; }
        }
    }
}
