using CrystalDecisions.CrystalReports.Engine;
using ResturantManagement.Reports;
using System;
using System.Collections;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.SqlClient;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Web.Services.Protocols;
using System.Windows.Forms;
using System.Data.SqlServerCe;
using Guna.UI2.WinForms;

namespace ResturantManagement.Model
{
    public partial class frmBillList : Form
    {
        SqlCeDataReader dr;
        public frmBillList()
        {
            InitializeComponent();
        }
        public int MainID = 0;
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
        private void frmBillList_Load(object sender, EventArgs e)
        {
            LoadData();
        }
        private void LoadData()
        {
            Boolean hascart = false;
            int i = 0;
            DateTime date = DateTime.Now;

            ClassConnection.con.Open();
            string qry = @"select MainID, TableName,WaiterName, orderType, status,
                            total from tblMain where status <> 'Pending' and aDate = '" + date.ToShortDateString() + "' ";
            SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
            dr = cm.ExecuteReader();
            DataTable dt = new DataTable();
            SqlCeDataAdapter da = new SqlCeDataAdapter(cm);
            da.Fill(dt);
            if (dt.Rows.Count <= 0)
            {
                //MessageBox.Show("No data found");
                lblNotData.Show();
                //showToast("WARNING", "Data not found");
            }
            else if (dt.Rows.Count > 0)
            {
                while (dr.Read())
                {
                    i++;
                    guna2DataGridView1.Rows.Add(i, dr["MainID"].ToString(),dr["TableName"].ToString(), dr["WaiterName"].ToString(), dr["orderType"].ToString(), dr["status"].ToString(), double.Parse(dr["total"].ToString()).ToString("#,##0.00"));
                    hascart = true;
                }
                dr.Close();
                ClassConnection.con.Close();
                lblNotData.Hide();
            }
            dr.Close();
            ClassConnection.con.Close();

            /* ListBox lb = new ListBox();

             lblNotData.Hide();
             lb.Items.Add(dgvid);
             lb.Items.Add(dgvtable);
             lb.Items.Add(dgvWaiter);
             lb.Items.Add(dgvType);
             lb.Items.Add(dgvStatus);
             lb.Items.Add(dgvTotal);
             ClassConnection.LoadData(qry, guna2DataGridView1, lb);*/
        }

        private void guna2DataGridView1_CellFormatting(object sender, DataGridViewCellFormattingEventArgs e)
        {
            // for searil no
            int count = 0;
            foreach (DataGridViewRow row in guna2DataGridView1.Rows)
            {
                count++;
                row.Cells[0].Value = count;
            }
        }

        private void guna2DataGridView1_CellContentClick_1(object sender, DataGridViewCellEventArgs e)
        {
            if (guna2DataGridView1.CurrentCell.OwningColumn.Name == "dgvedit")
            {
                // it is change as we have to set form text propties befor open
                MainID = Convert.ToInt32(guna2DataGridView1.CurrentRow.Cells["dgvid"].Value);
                this.Close();
            }

            if (guna2DataGridView1.CurrentCell.OwningColumn.Name == "dgvdel")
            {
                // Print Bill
                MainID = Convert.ToInt32(guna2DataGridView1.CurrentRow.Cells["dgvid"].Value);
                ClassConnection.con.Open();
                string qry = @"select * from tblMain m inner join
                            tblDetails d on d.MainID = m.MainID
                            inner join products p on p.pID = d.proID
                            where m.MainID = " + MainID + "";
                SqlCeCommand cmd = new SqlCeCommand(qry, ClassConnection.con);
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
    }
}
