using Guna.UI2.WinForms;
using Microsoft.Reporting.Map.WebForms.BingMaps;
using Microsoft.SqlServer.Server;
using ResturantManagement.Reports;
using ResturantManagement.View;
//using Microsoft.ReportingServices.ReportProcessing.ReportObjectModel;
using System;
using System.Collections;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.SqlClient;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Runtime.Versioning;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using static System.Windows.Forms.VisualStyles.VisualStyleElement;
using static System.Windows.Forms.VisualStyles.VisualStyleElement.Header;
using static System.Windows.Forms.VisualStyles.VisualStyleElement.ListView;
using System.Data.SqlServerCe;

namespace ResturantManagement.Model
{
    public partial class frmPOS : Form
    {
        SqlCeDataReader dr;
        public frmPOS()
        {
            InitializeComponent();
            lblTimer.Text = DateTime.Now.ToString("dddd, dd MMMM yyyy");
            lblDate.Text = DateTime.Now.ToLongDateString();
            GetTranNo();
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

        /* public void Alert(string msg, AlertMessage.enmType type)
         {
             AlertMessage alert = new AlertMessage();
             alert.showAlert(msg, type);
         }*/
        public void showToast(string type, string message)
        {
            ToasForm toas = new ToasForm(type, message);
            toas.Show();
        }

        public int MainID = 0;
        public string OrderType = "";
        public int driverID = 0;
        public string customerName = "";
        public string customerPhone = "";
        public string customerAddress = "";
        private void btnExit_Click(object sender, EventArgs e)
        {
            this.Close();
        }

        private void frmPOS_Load(object sender, EventArgs e)
        {
            dgvShowItems.BorderStyle = BorderStyle.FixedSingle;
            AddCategory();

            ProductPanel.Controls.Clear();
            LoadProcducts();

            // for text animation
            txts = lblTxt.Text;
            mimic = txts.Length;
            lblTxt.Text = "";
        }

        private void AddCategory()
        {
            string qry = "select * from Category ";
            SqlCeCommand cmd = new SqlCeCommand(qry, ClassConnection.con);
            SqlCeDataAdapter da = new SqlCeDataAdapter(cmd);
            DataTable dt = new DataTable();
            da.Fill(dt);

            CategoryPanel.Controls.Clear();
            Guna.UI2.WinForms.Guna2Button b2 = new Guna.UI2.WinForms.Guna2Button();
            b2.Checked = true;
            b2.FillColor = Color.FromArgb(50, 55, 89);
            b2.Size = new Size(149, 45);
            b2.ButtonMode = Guna.UI2.WinForms.Enums.ButtonMode.RadioButton;
            b2.Text = "មុខម្ហូបទាំងអស់"; 
            b2.Font = new Font("Khmer OS Battambang", 13);
            b2.CheckedState.FillColor = Color.FromArgb(255, 139, 19);
            b2.Click += new EventHandler(b_click);
            b2.BorderRadius = 5;
            b2.AutoSize = false;
            b2.Animated = true;
            b2.TextAlign = HorizontalAlignment.Center;
            CategoryPanel.Controls.Add(b2);
            

            if (dt.Rows.Count > 0)
            {
                foreach(DataRow row in dt.Rows)
                {
                    Guna.UI2.WinForms.Guna2Button b = new Guna.UI2.WinForms.Guna2Button();
                    b.FillColor = Color.FromArgb(50, 55, 89);
                    b.Size = new Size(149, 45);
                    b.ButtonMode = Guna.UI2.WinForms.Enums.ButtonMode.RadioButton;
                    b.Text = row["catName"].ToString();
                    b.Font = new Font("Khmer OS Battambang", 13);
                    b.CheckedState.FillColor = Color.FromArgb(255, 139, 19);
                    b.BorderRadius = 5;
                    b.AutoSize = false;
                    b.Animated = true;
                    b.TextAlign = HorizontalAlignment.Left;
                    // event for click
                    b.Click += new EventHandler(b_click);

                    CategoryPanel.Controls.Add(b);
                }
            }
            
        }

        private void b_click(object sender, EventArgs e)
        {
            Guna.UI2.WinForms.Guna2Button b = (Guna.UI2.WinForms.Guna2Button)sender;
            if(b.Text == "មុខម្ហូបទាំងអស់")
            {
                txtSearch.Text = "1";
                txtSearch.Text = "";
                return;
            }

            foreach (var item in ProductPanel.Controls)
            {
                var pro = (usProduct)item;
                pro.Visible = pro.PCategory.ToLower().Contains(b.Text.Trim().ToLower());

            }
        }

        // add item from click pic
        private void AddItems(string id, String proID ,string name , string cat, string price, Image pimage)
        {
            var w = new usProduct()
            {
                PName = name,
                PPrice = price,
                pPrice = price, // for get price in upProduct
                PCategory = cat,
                PImage = pimage,

                id = Convert.ToInt32(proID)
            };

            ProductPanel.Controls.Add(w);

            w.onSelect += (ss, ee) =>
            {
                var wdg = (usProduct)ss;
                foreach (DataGridViewRow item in dgvShowItems.Rows)
                {
                    if (Convert.ToInt32(item.Cells["dgvproID"].Value) == wdg.id)
                    {
                        // this will check it product alreay there then a one to quantity and update price
                        item.Cells["dgvQty"].Value = int.Parse(item.Cells["dgvQty"].Value.ToString()) + 1;
                        item.Cells["dgvAmount"].Value = (int.Parse(item.Cells["dgvQty"].Value.ToString()) *
                                                        double.Parse(item.Cells["dgvPrice"].Value.ToString()));

                        GetTotal();
                        return;
                    }
                }
                // this line add new product first for No# and 2nd 0 from id
                dgvShowItems.Rows.Add(new object[] {0, 0, wdg.id, wdg.PName, 1, wdg.PPrice, wdg.PPrice , wdg.pPrice});
                GetTotal();
            };
        }

        // geting Product from database
        private void LoadProcducts()
        {
            string qry = "select * from products inner join category on catID = CategoryID ";

            SqlCeCommand cmd = new SqlCeCommand(qry, ClassConnection.con);
            SqlCeDataAdapter da = new SqlCeDataAdapter(cmd);
            DataTable dt = new DataTable();
            da.Fill(dt);

            if(dt.Rows.Count > 0)
            {
                foreach (DataRow item in dt.Rows)
                {
                    Byte[] imagearray = (byte[])item["pImage"];
                    byte[] imagebytearray = imagearray;

                    AddItems("0", item["pID"].ToString(), item["pName"].ToString(), item["catName"].ToString(),
                        item["pPrice"].ToString(), Image.FromStream(new MemoryStream(imagearray)));
                }
            }
        }

        private void txtSearch_TextChanged(object sender, EventArgs e)
        {
            foreach(var item in ProductPanel.Controls)
            {
                var pro = (usProduct)item;
                pro.Visible = pro.PName.ToLower().Contains(txtSearch.Text.Trim().ToLower());

            }
        }

        private void guna2DataGridView1_CellFormatting(object sender, DataGridViewCellFormattingEventArgs e)
        {
            // for searil no
            int count = 0;
            foreach (DataGridViewRow row in dgvShowItems.Rows)
            {
                count++;
                row.Cells[0].Value = count;
            }
        }

        // get Total
        private void GetTotal()
        {
            double tot = 0;
            double vat = 0;
            double totalUS = 0;
            double totalKH = 0;
            double total_kh = 0;
            double disc = 0;
            lblTotalUS.Text = "" ;
            lblTotalKH.Text = "" ;
            // start laod currency from db
            SqlCeDataReader dr;
            ClassConnection.con.Open();
            string qry = "select * from tblCurrency";
            SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
            dr = cm.ExecuteReader();
            if (dr.Read())
            {
                lblTotalKH.Text = dr["cuChange"].ToString();
            }
            dr.Close();
            ClassConnection.con.Close();
            // end laod currency from db

            double.TryParse(lblTotalKH.Text, out totalKH);
            foreach (DataGridViewRow item in dgvShowItems.Rows)
            {
                    tot += double.Parse(item.Cells["dgvAmount"].Value.ToString());

                    totalUS = Math.Abs(tot - disc);
                    total_kh = Math.Abs(tot * totalKH);

                lblTotalUS.Text = tot.ToString("#,##0.00") ;
                lblTotalKH.Text = total_kh.ToString("#,##0");

            }
        }

        private void btnNew_Click(object sender, EventArgs e)
        {
            btnDin.Checked      = false;
            btnBill.Checked     = false;
            btnKot.Checked      = false;
            btnDelivery.Checked = false;
            btnDin.Checked      = false;

            lblDriverName.Text = "";
            lblTable.Text = "";
            lblWaiter.Text = "";
            lblTable.Visible = false;
            lblWaiter.Visible = false;
            dgvShowItems.Rows.Clear();
            MainID = 0;
            lblTotalUS.Text = "00";
            lblTotalKH.Text = "00";
            GetTranNo();
        }

        private void btnDelivery_Click(object sender, EventArgs e)
        {
            btnDin.Checked = false;
            btnBill.Checked = false;
            btnKot.Checked = false;
            btnDelivery.Checked = true;
            btnDin.Checked = false;

            lblTable.Text = "";
            lblWaiter.Text = "";
            lblTable.Visible = false;
            lblWaiter.Visible = false;
            OrderType = "Delivery";

            frmAddCustomer frm = new frmAddCustomer();
            frm.mainID = MainID;
            frm.orderType = OrderType;
            ClassConnection.BlueBackground(frm);

            if (frm.txtPhoneDriver.Text != "") //as take away did not have driver info
            {
                driverID = frm.driverID;
                lblDriverName.Text = "Customer's Phone: " + frm.txtPhone.Text + " Driver: " + frm.cbDriver.Text  + " Driver's Phone: " + frm.txtPhoneDriver.Text + 
                   "\n Address's Customer: " +frm.txtAddress.Text;
                lblDriverName.Visible = true;
                customerName = frm.txtPhoneDriver.Text;
                customerPhone = frm.txtPhone.Text;
                customerAddress = frm.txtAddress.Text;
            }
        }

        private void btnDin_Click(object sender, EventArgs e)
        {
            btnDin.Checked = false;
            btnBill.Checked = false;
            btnKot.Checked = false;
            btnDelivery.Checked = false;
            btnDin.Checked = true;

            OrderType = "Din In";
            lblDriverName.Visible = false;
            // need to create form for table selection and waiter selection
            frmTableSelect frm = new frmTableSelect();
            ClassConnection.BlueBackground(frm);
            
            if(frm.TableName != "")
            {
                lblTable.Text = frm.TableName;
                lblTable.Visible = true;
            }
            else
            {
                lblTable.Text = "";
                lblTable.Visible = false;
            }

            frmWaiterSelect frmWaiter = new frmWaiterSelect();
            ClassConnection.BlueBackground(frmWaiter);
            if (frmWaiter.waiterName != "")
            {
                lblWaiter.Text = frmWaiter.waiterName;
                lblWaiter.Visible = true;
            }
            else
            {
                lblWaiter.Text = "";
                lblWaiter.Visible = false;
            }
        }
        // get invoiceNo
        public void GetTranNo()
        {
            
            string sdate = DateTime.Now.ToString("yyyyMMdd");
            int count;
            string transno;
            ClassConnection.con.Open();
            string qry = "SELECT TOP 1 invoiceNo FROM tblMain WHERE invoiceNo LIKE '" + sdate + "%' ORDER BY MainID desc";
            SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
            dr = cm.ExecuteReader();
            //dr.Read();
            if (dr.Read())
            {
                transno = dr[0].ToString();
                count = int.Parse(transno.Substring(8, 4));
                lblTranNo.Text = sdate + (count + 1);
            }
            else
            {
                transno = sdate + "1001";
                lblTranNo.Text = transno;
            }
            dr.Close();
            ClassConnection.con.Close();
        }
        private void btnKot_Click(object sender, EventArgs e)
        {
            btnDin.Checked = false;
            btnBill.Checked = false;
            btnKot.Checked = true;
            btnDelivery.Checked = false;
            btnDin.Checked = false;

            string qry1 = ""; //Main table
            string qry2 = ""; 
            int detailID = 0;
            double total = 0;
            double discount = 0;

            if (OrderType == "" || dgvShowItems.Rows.Count <=0 )
            {
                //this.Alert("Please selece order type", AlertMessage.enmType.Error);
                showToast("ERROR", "Please selece order type");
                return;
            }

            if (MainID == 0) // Insert
            {
                qry1 = @"insert into tblMain (invoiceNo,aDate, aTime, TableName,WaiterName,
                       status,orderType ,total,totalKH,discount,netAmt, received, change,changeKh, driverID, CustName, CustPhone,CustAddr) 
                       values(@invoiceNo,@aDate, @aTime, @TableName,@WaiterName,
                       @status,@orderType ,@total,@totalKH,@discount,@netAmt, @received, @change,@changeKh, @driverID, @CustName, @CustPhone,@CustAddr); 
                       ";
            }
            else //Update
            {
                qry1 = @"update tblMain set status = @status,orderType = @orderType,invoiceNo=@invoiceNo ,discount = @discount,netAmt=@netAmt, total = @total,
                         totalKH=@totalKH, received = @received, change = @change, changeKh= @changeKh where MainID = @ID";
            }

            Hashtable ht = new Hashtable();

            SqlCeCommand cmd = new SqlCeCommand(qry1, ClassConnection.con);
            cmd.Parameters.AddWithValue("@ID",MainID);
            cmd.Parameters.AddWithValue("@invoiceNo", lblTranNo.Text);
            cmd.Parameters.AddWithValue("@aDate",Convert.ToDateTime(DateTime.Now.Date));
            cmd.Parameters.AddWithValue("@aTime",DateTime.Now.ToShortTimeString());
            cmd.Parameters.AddWithValue("@TableName",lblTable.Text);
            cmd.Parameters.AddWithValue("@WaiterName",lblWaiter.Text);
            cmd.Parameters.AddWithValue("@status","Pending");
            cmd.Parameters.AddWithValue("@orderType",OrderType);
            cmd.Parameters.AddWithValue("@discount", Convert.ToDouble(0));
            cmd.Parameters.AddWithValue("@netAmt", Convert.ToDouble(lblTotalUS.Text));
            cmd.Parameters.AddWithValue("@total",Convert.ToDouble(lblTotalUS.Text)); // as we only saving date for kitchen value will update when payment recieved
            cmd.Parameters.AddWithValue("@totalKH", Convert.ToDouble(lblTotalKH.Text));
            cmd.Parameters.AddWithValue("@received", Convert.ToDouble(0));
            cmd.Parameters.AddWithValue("@change", Convert.ToDouble(0));
            cmd.Parameters.AddWithValue("@changeKh", Convert.ToDouble(0));
            cmd.Parameters.AddWithValue("@driverID", driverID);
            cmd.Parameters.AddWithValue("@CustName", customerName);
            cmd.Parameters.AddWithValue("@CustPhone", customerPhone);
            cmd.Parameters.AddWithValue("@CustAddr", customerAddress);

            updateStatustableUnavilible();//update status table to មិនទំនេរ

            if (ClassConnection.con.State == ConnectionState.Closed) { ClassConnection.con.Open(); }
            if(MainID == 0) { 
                MainID = Convert.ToInt32(cmd.ExecuteScalar());
                string query = "select max(MainID) from tblMain ;";
                SqlCeCommand cm = new SqlCeCommand(query, ClassConnection.con);
                dr = cm.ExecuteReader();
                if (dr.Read())
                {
                    MainID = Convert.ToInt32(dr[0].ToString());
                
                }
            } else { cmd.ExecuteNonQuery(); }
            if(ClassConnection.con.State == ConnectionState.Open) { ClassConnection.con.Close(); }

            foreach(DataGridViewRow row in dgvShowItems.Rows)
            {
                detailID = Convert.ToInt32(row.Cells["dgvid"].Value);
                if(detailID == 0) // insert
                {
                    qry2 = @"insert into tblDetails (MainID,proId,qty,price,amount) values(@MainID,@proID, @qty, @price, @amount)";
                }                                    
                else // Update                        
                {
                    qry2 = @"update tblDetails set proID = @proID, qty = @qty, price = @price, amount =@amount
                             where DetailID = @ID";
                }


                SqlCeCommand cmd2 = new SqlCeCommand(qry2, ClassConnection.con);
                cmd2.Parameters.AddWithValue("@ID", MainID);
                cmd2.Parameters.AddWithValue("@MainID",MainID);
                cmd2.Parameters.AddWithValue("@proID",Convert.ToInt32( row.Cells["dgvproID"].Value));
                cmd2.Parameters.AddWithValue("@qty", Convert.ToInt32(row.Cells["dgvQty"].Value));
                cmd2.Parameters.AddWithValue("@price", Convert.ToDouble(row.Cells["dgvPrice"].Value));
                cmd2.Parameters.AddWithValue("@amount", Convert.ToDouble(row.Cells["dgvAmount"].Value));

                if (ClassConnection.con.State == ConnectionState.Closed) { ClassConnection.con.Open(); }
                cmd2.ExecuteNonQuery(); 
                if (ClassConnection.con.State == ConnectionState.Open) { ClassConnection.con.Close(); }

            }
            //this.Alert("Saved successfully.", AlertMessage.enmType.Success);
            showToast("SUCCESS", "Saved Successfully.");
            //guna2MessageDialog1.Show("Saved successfully");
            MainID = 0;
            detailID = 0;
            dgvShowItems.Rows.Clear();
            lblTable.Text = "";
            lblWaiter.Text = "";
            lblTable.Visible = false;
            lblWaiter.Visible = false;
            lblTotalUS.Text = "00";
            lblTotalKH.Text = "00";
            lblDriverName.Text = "";
            GetTranNo();
        }

        public int id = 0;
        private void btnBill_Click(object sender, EventArgs e)
        {
            btnDin.Checked = false;
            btnBill.Checked = true;
            btnKot.Checked = false;
            btnDelivery.Checked = false;
            btnDin.Checked = false;

            frmBillList frm = new frmBillList();
            ClassConnection.BlueBackground(frm);
            if (frm.MainID > 0)
            {
                id = frm.MainID;
                MainID = frm.MainID;
                LoadEnteries();
            }
        }

        private void LoadEnteries()
        {
            string qry = @"Select * from tblDetails d
                         inner join tblMain m on d.MainID = m.MainID
                         inner join products p on p.pID = d.proID
                         Where m.MainID = " + id + "";
            SqlCeCommand cmd2 = new SqlCeCommand(qry, ClassConnection.con);
            DataTable dt2 = new DataTable();
            SqlCeDataAdapter da2 = new SqlCeDataAdapter(cmd2);
            da2.Fill(dt2);

            if (dt2.Rows[0]["orderType"].ToString() == "Delivery")
            {
                btnDelivery.Checked = true;
                lblWaiter.Visible = false;
                lblTable.Visible = false;
            }
            else if(dt2.Rows[0]["orderType"].ToString() == "Take away")
            {
                lblWaiter.Visible = false;
                lblTable.Visible = false;
            }
            else
            {
                btnBill.Checked = false;
                btnDin.Checked = true;
                lblWaiter.Visible = true;
                lblTable.Visible = true;
            }

            dgvShowItems.Rows.Clear();

            foreach(DataRow item in dt2.Rows)
            {
                lblTable.Text = item["TableName"].ToString();
                lblWaiter.Text = item["WaiterName"].ToString();

                string detailid = item["DetailID"].ToString();
                string proName  = item["pName"].ToString();
                string proid    = item["proID"].ToString();
                string qty      = item["qty"].ToString();
                string price    = item["price"].ToString();
                string amount   = item["amount"].ToString();

                object[] obj = { 0, detailid,  proid, proName, qty, price, amount };
                dgvShowItems.Rows.Add(obj);
            }
            GetTotal();
        }
        // btn checkout
        private void btnCheckout_Click(object sender, EventArgs e)
        {

            frmCheckout frm = new frmCheckout(lblTable.Text);
            frm.MainID = id;
            frm.amt = Convert.ToDouble(lblTotalUS.Text);
            frm.totKh= Convert.ToDouble(lblTotalKH.Text);
            ClassConnection.BlueBackground(frm);

            MainID = 0;
            dgvShowItems.Rows.Clear();
            lblTable.Text = "";
            lblWaiter.Text = "";
            lblTable.Visible = false;
            lblWaiter.Visible = false;
            lblTotalUS.Text = "00";
            lblTotalKH.Text = "00";
        }
        private void timer1_Tick(object sender, EventArgs e)
        {
            lblTimer.Text = DateTime.Now.ToString("hh:mm:ss tt");
        }

        private void guna2DataGridView1_CellContentClick_1(object sender, DataGridViewCellEventArgs e)
        {
            // need to confirm befor delete data
            if (dgvShowItems.CurrentCell.OwningColumn.Name == "dgvDel")
            {
                guna2MessageDialog1.Icon = Guna.UI2.WinForms.MessageDialogIcon.Information;
                guna2MessageDialog1.Buttons = Guna.UI2.WinForms.MessageDialogButtons.YesNo;

                if (guna2MessageDialog1.Show("Are you sure want to delete?") == DialogResult.Yes)
                {
                    int rowindex = dgvShowItems.CurrentCell.RowIndex;
                    dgvShowItems.Rows.RemoveAt(rowindex);

                    int id = Convert.ToInt32(dgvShowItems.CurrentRow.Cells["dgvid"].Value);
                    string qry = "Delete from category where catID = '" + id + "'";
                    Hashtable ht = new Hashtable();
                    ClassConnection.SQl(qry, ht);

                    //guna2MessageDialog1.Show("Deleted Successfull");
                    GetTotal();
                }
            }
        }

        // btn print menu
        private void btnPrintMenu_Click(object sender, EventArgs e)
        {
            string qry = @"select * from products";
            SqlCeCommand cmd = new SqlCeCommand(qry, ClassConnection.con);
            ClassConnection.con.Open();
            DataTable dt = new DataTable();
            SqlCeDataAdapter da = new SqlCeDataAdapter(cmd);
            da.Fill(dt);
            ClassConnection.con.Close();
            frmPrint frm = new frmPrint();
            rptMenu cr = new rptMenu();

            cr.SetDatabaseLogon("sa", "sql123");
            cr.SetDataSource(dt);
            frm.crystalReportViewer1.ReportSource = cr;
            frm.crystalReportViewer1.Refresh();
            frm.Show();
        }
        // btn fast cash
        private void btnFastCashe_Click(object sender, EventArgs e)
        {
            btnDin.Checked = false;
            btnBill.Checked = false;
            btnKot.Checked = false;
            btnDelivery.Checked = false;
            btnDin.Checked = false;

            updateStatustableAvilible();//update table to ទំនេរ
             if (MainID == 0)
            {
                //this.Alert("No Items! please choose items", AlertMessage.enmType.Error);
                showToast("ERROR", "No Items! please choose items.");
                return;
            }

                /*foreach (DataGridViewRow row in dgvShowItems.Rows)
                {*/
                //MainID = Convert.ToInt32(row.Cells["dgvid"].Value);
                string qry = @"update tblMain set total=@total,totalKH=@totalKH,netAmt=@netAmt,changeKh=@changeKh,discount=@discount, received = @rec, change = @change ,
                            status = 'Paid' where MainID = @id";
                    SqlCeCommand cmd = new SqlCeCommand(qry, ClassConnection.con);
                    cmd.Parameters.AddWithValue("@id", MainID);
                    cmd.Parameters.AddWithValue("@total", Convert.ToDouble(lblTotalUS.Text));
                    cmd.Parameters.AddWithValue("@totalKH", Convert.ToDouble(lblTotalKH.Text));
                    cmd.Parameters.AddWithValue("@netAmt", Convert.ToDouble(lblTotalUS.Text));
                    cmd.Parameters.AddWithValue("@change", Convert.ToDouble(0));
                    cmd.Parameters.AddWithValue("@changeKh", Convert.ToDouble(0));
                    //cmd.Parameters.AddWithValue("@qty", Convert.ToInt32(row.Cells["dgvQty"].Value));
                    cmd.Parameters.AddWithValue("@rec", Convert.ToDouble(0));
                    cmd.Parameters.AddWithValue("@discount", Convert.ToDouble(0));

                    if (ClassConnection.con.State == ConnectionState.Closed) { ClassConnection.con.Open(); }
                    cmd.ExecuteNonQuery();
                    if (ClassConnection.con.State == ConnectionState.Open) { ClassConnection.con.Close(); }

                    ClassConnection.con.Open();
                    string qry1 = @"select * from tblMain m inner join
                                tblDetails d on d.MainID = m.MainID
                                inner join products p on p.pID = d.proID
                                where m.MainID = " + MainID + "";
                    SqlCeCommand cmd1 = new SqlCeCommand(qry1, ClassConnection.con);
                    SqlCeDataAdapter da = new SqlCeDataAdapter(cmd1);
                    System.Data.DataTable dt = new System.Data.DataTable();
                    da.Fill(dt);
                    frmPrint frm = new frmPrint();
                    Invoice cr = new Invoice();

                    cr.SetDataSource(dt);
                    frm.crystalReportViewer1.ReportSource = cr;
                    frm.crystalReportViewer1.Refresh();
                    frm.Show();
                    ClassConnection.con.Close();
            //}
            //this.Alert("Saved successfully.", AlertMessage.enmType.Success);
            showToast("SUCCESS", "Saved Successfully.");
            MainID = 0;
            dgvShowItems.Rows.Clear();
            lblTable.Text = "";
            lblWaiter.Text = "";
            lblTable.Visible = false;
            lblWaiter.Visible = false;
            lblTotalUS.Text = "0.00";
            lblTotalKH.Text = "0.00";
            lblDriverName.Text = "";
            GetTranNo();
        }

        public void updateStatustableUnavilible()
        {

            // for set defaul status table
            //MessageBox.Show(lblTable.Text);
            ClassConnection.con.Open();
            string upate = "update tables set status=@status, tname=@tname where tname = '" + lblTable.Text + "'";
            SqlCeCommand cmd2 = new SqlCeCommand(upate, ClassConnection.con);
            cmd2.Parameters.AddWithValue("@tname", lblTable.Text);
            cmd2.Parameters.AddWithValue("@status", "មិនទំនេរ");
            cmd2.ExecuteNonQuery();
            ClassConnection.con.Close();
        }
        public void updateStatustableAvilible()
        {

            // for set defaul status table
            //MessageBox.Show(lblTable.Text);
            ClassConnection.con.Open();
            string upate = "update tables set status=@status, tname=@tname where tname = '" + lblTable.Text + "'";
            SqlCeCommand cmd2 = new SqlCeCommand(upate, ClassConnection.con);
            cmd2.Parameters.AddWithValue("@tname", lblTable.Text);
            cmd2.Parameters.AddWithValue("@status", "ទំនេរ");
            cmd2.ExecuteNonQuery();
            ClassConnection.con.Close();
        }
        int counter = 0, mimic = 0;
        string txts;
        private void timer2_Tick(object sender, EventArgs e)
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
                    //lblTxt.ForeColor = Color.Teal;
                    lblTxt.ForeColor = Color.White;
            }
        }
    }                                              
}                                                 
                                                     
                                                   
                                                       
                                                   
                                                      