package vn.brine.haileader.expolatorysearch.asynctasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;

import vn.brine.haileader.expolatorysearch.utils.Config;

/**
 * Created by HaiLeader on 7/15/2016.
 */
public class SearchDataWiki extends AsyncTask<String, Void, ResultSet> {

    public final static String TAG = "SearchDataFreeBase";
    private OnTaskCompleted mOnTaskCompleted;
    private int mTypeSearch;
    private Context mContext;
    private static ProgressDialog mProgressDialog;

    public SearchDataWiki(Context context, OnTaskCompleted onTaskCompleted, int typeSearch){
        this.mContext = context;
        this.mOnTaskCompleted = onTaskCompleted;
        this.mTypeSearch = typeSearch;
        mProgressDialog = new ProgressDialog(mContext);
    }

    public interface OnTaskCompleted{
        void onAsyncTaskCompletedWiki(ResultSet resultSet, int typeSearch);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.show();
    }

    @Override
    protected ResultSet doInBackground(String... params) {
        String queryString = params[0];
        Log.d(TAG, queryString);
        Query query = QueryFactory.create(queryString);
        QueryExecution queryExecution = QueryExecutionFactory.createServiceRequest(Config.WIKIDATA_ENDPOINT, query);
        ResultSet resultSet = queryExecution.execSelect();
        queryExecution.close();
        return resultSet;
    }

    @Override
    protected void onPostExecute(ResultSet resultSet) {
        super.onPostExecute(resultSet);
        if(mProgressDialog.isShowing()){
            mProgressDialog.dismiss();
        }
        mOnTaskCompleted.onAsyncTaskCompletedWiki(resultSet, mTypeSearch);
    }
}
