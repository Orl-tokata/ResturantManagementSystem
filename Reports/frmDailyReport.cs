using Guna.UI2.WinForms;
using Microsoft.Office.Interop.Excel;
using Microsoft.Reporting.Map.WebForms.BingMaps;
using ResturantManagement.View;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.SqlClient;
using System.Drawing;
using System.Linq;
using System.Reflection;
using System.Security.Cryptography;
using System.Security.Policy;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using static System.Windows.Forms.AxHost;
using System.Data.SqlServerCe;
using System.IO;
using System.Web.Security;
using System.Xml.Linq;
using CrystalDecisions.CrystalReports.Engine;

namespace ResturantManagement.Reports
{
    public partial class frmDailyReport : Form
    {
        SqlCeDataReader dr;
        public frmDailyReport()
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
        public void showToast(string type, string message)
        {
            ToasForm toas = new ToasForm(type, message);
            toas.Show();
        }
        public double ExtractData(string sql)
        {
            ClassConnection.con.Open();
            SqlCeCommand cm = new SqlCeCommand(sql, ClassConnection.con);
            double data = double.Parse(cm.ExecuteScalar().ToString());
            ClassConnection.con.Close();
            return data;

        }
        private void dgvCash_CellFormatting(object sender, DataGridViewCellFormattingEventArgs e)
        {
            // for searil no
            int count = 0;
            foreach (DataGridViewRow row in dgvDailyReport.Rows)
            {
                count++;
                row.Cells[0].Value = count;
            }
        }
        private void frmDailyReport_Load_1(object sender, EventArgs e)
        {
            string sdate = DateTime.Now.ToShortDateString();
            string fromDt = dtFromDaily.Value.ToShortDateString();
            string toDt = dtToDaily.Value.ToShortDateString();
            //lblTotalUSD.Text = ExtractData("SELECT COALESCE(sum(COALESCE(netAmt,0)),0) AS total FROM tblMain WHERE status LIKE 'Paid' AND aDate BETWEEN '" + fromDt + "' AND '" + toDt + "'").ToString("#,##0.00") + " $ ";
            //lblTotalKH.Text = ExtractData("SELECT COALESCE(sum(COALESCE(totalKH,0)),0) AS total FROM tblMain WHERE status LIKE 'Paid' AND aDate BETWEEN '" + fromDt + "' AND '" + toDt + "'").ToString("#,##0") + " ៛ ";
            
        }
        // btn load data daily sale
        public string totalUS;
        public string totalKHR;
        private void btnLoadData_Click(object sender, EventArgs e)
        {
            Boolean hascart = false;
            int i = 0;
            dgvDailyReport.Rows.Clear();

            string fromDt = dtFromDaily.Value.ToShortDateString();
            string toDt = dtToDaily.Value.ToShortDateString();

            if (Convert.ToDateTime(fromDt) > Convert.ToDateTime(toDt))
            {
                //this.Alert("From Date must be lesster \nthan To Date", AlertMessage.enmType.Error);
                showToast("ERROR", "From Date must be lesster than To Date");
            }

            lblTotalUSD.Text = ExtractData("SELECT COALESCE(sum(COALESCE(netAmt,0)),0) AS total FROM tblMain WHERE status LIKE 'Paid' AND aDate BETWEEN '" + fromDt + "' AND '" + toDt + "'").ToString("#,##0.00") + " $ ";
            lblTotalKH.Text = ExtractData("SELECT COALESCE(sum(COALESCE(totalKH,0)),0) AS total FROM tblMain WHERE status LIKE 'Paid' AND aDate BETWEEN '" + fromDt + "' AND '" + toDt + "'").ToString("#,##0") + " ៛ ";
            
            ClassConnection.con.Open();
            string qry = "select m.aDate, m.aTime,m.invoiceNo,m.orderType, m.TableName, m.status, m.total,m.discount, m.netAmt,m.totalKH from tblMain m inner join tblDetails d on d.MainID = m.MainID inner join products p on p.pID = d.proID  where status like 'Paid' and m.aDate  between '" + fromDt + "' and '" + toDt + "' GROUP BY m.aDate, m.aTime, m.invoiceNo,m.orderType,m.TableName, m.status, m.total ,m.discount, m.netAmt,m.totalKH order by m.TableName";
            SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
            dr = cm.ExecuteReader();
            System.Data.DataTable dt = new System.Data.DataTable();
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
                    var datef = DateTime.Parse(dr["aDate"].ToString());
                    dgvDailyReport.Rows.Add(i, datef.ToString("MM/dd/yyyy"), dr["aTime"].ToString(), dr["invoiceNo"].ToString(), dr["orderType"].ToString(), dr["TableName"].ToString(), dr["status"].ToString(), double.Parse(dr["total"].ToString()).ToString("#,##0.00"), double.Parse(dr["discount"].ToString()), double.Parse(dr["netAmt"].ToString()).ToString("#,##0.00"), double.Parse(dr["totalKH"].ToString()).ToString("#,##0"));//
                    hascart = true;
                }
                dr.Close();
                ClassConnection.con.Close();
                lblNotData.Hide();
            }
            dr.Close();
            ClassConnection.con.Close();
        }
        // btn print daily sale
        private void btnPrint_Click(object sender, EventArgs e)
        {
            ClassConnection.con.Open();
            string fromDt = dtFromDaily.Value.ToShortDateString();
            string toDt = dtToDaily.Value.ToShortDateString();
            string sdate = DateTime.Now.ToShortDateString();
            string qry = @"select m.aDate, m.aTime, m.invoiceNo, m.TableName,m.orderType, m.status, m.total,m.discount, m.netAmt,m.totalKH
                            from tblMain m 
                            inner join tblDetails d on d.MainID = m.MainID 
                            inner join products p on p.pID = d.proID 
                            where status like 'Paid' and m.aDate between '" + fromDt + "' and '" + toDt + "' GROUP BY m.aDate, m.aTime,m.invoiceNo, m.TableName,m.orderType, m.status, m.total ,m.discount, m.netAmt,m.totalKH order by m.TableName";

            SqlCeCommand cmd = new SqlCeCommand(qry, ClassConnection.con);
            SqlCeDataAdapter da = new SqlCeDataAdapter(cmd);
            System.Data.DataTable dt = new System.Data.DataTable();
            da.Fill(dt);
            if (dgvDailyReport.Rows.Count <= 0)
            {
                //MessageBox.Show("No data found");
                lblNotData.Show();
                //showToast("WARNING", "Data not found");
            }
            else if (dgvDailyReport.Rows.Count > 0)
            {
                frmPrint frm = new frmPrint();
                rptDailyReport cr = new rptDailyReport();
                cr.SetDataSource(dt);
                frm.crystalReportViewer1.ReportSource = cr;
                frm.crystalReportViewer1.Refresh();
                frm.Show();
                lblNotData.Hide();
                ClassConnection.con.Close();
            }
            ClassConnection.con.Close();
        }
        // btn download to excel
        private void btnExport_Click(object sender, EventArgs e)
        {
            /*dgvDailyReport.SelectAll();
            DataObject copydata = dgvDailyReport.GetClipboardContent();
            if (copydata != null) Clipboard.SetDataObject(copydata);
            Microsoft.Office.Interop.Excel.Application xlapp = new Microsoft.Office.Interop.Excel.Application();
            xlapp.Visible = true;
            Microsoft.Office.Interop.Excel.Workbook xlwbook;
            Microsoft.Office.Interop.Excel.Worksheet xlsheet;
            object miseddata = System.Reflection.Missing.Value;
            xlwbook = xlapp.Workbooks.Add(miseddata);

            xlsheet = (Microsoft.Office.Interop.Excel.Worksheet)xlwbook.Worksheets.get_Item(1);
            Microsoft.Office.Interop.Excel.Range xlr = (Microsoft.Office.Interop.Excel.Range)xlsheet.Cells[1, 1];
            xlr.Select();

            xlsheet.PasteSpecial(xlr, Type.Missing, Type.Missing, Type.Missing, Type.Missing, Type.Missing, true);*/
            
            if(dgvDailyReport.Rows.Count > 0)
            {
                Microsoft.Office.Interop.Excel.Application excelApp = new Microsoft.Office.Interop.Excel.Application();
                excelApp.Application.Workbooks.Add(Type.Missing);
                for(int i = 1; i < dgvDailyReport.Columns.Count + 1; i++)
                {
                    excelApp.Cells[1, i] = dgvDailyReport.Columns[i - 1].HeaderText;
                }
                for(int i = 0; i < dgvDailyReport.Rows.Count; i++)
                {
                    for(int j = 0; j < dgvDailyReport.Columns.Count; j++)
                    {
                        excelApp.Cells[i + 2, j + 1] = dgvDailyReport.Rows[i].Cells[j].Value.ToString();
                    }
                }
                excelApp.Columns.AutoFit();
                excelApp.Visible = true;
                lblNotData.Hide();
            }
            else
            {
                //this.Alert("Data not found", AlertMessage.enmType.Error);
                //showToast("WARNING", "Data not found");
                lblNotData.Show();
            }
        }
    }
}
