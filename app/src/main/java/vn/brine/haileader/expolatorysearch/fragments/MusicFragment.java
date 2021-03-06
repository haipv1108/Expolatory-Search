package vn.brine.haileader.expolatorysearch.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.cunoraz.tagview.Tag;
import com.cunoraz.tagview.TagView;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

import java.util.ArrayList;
import java.util.List;

import vn.brine.haileader.expolatorysearch.R;
import vn.brine.haileader.expolatorysearch.adapter.SearchResultAdapter;
import vn.brine.haileader.expolatorysearch.dbpedia.asynctasks.SlidingWindowAsynctask;
import vn.brine.haileader.expolatorysearch.models.DividerItemDecoration;
import vn.brine.haileader.expolatorysearch.models.SearchResult;
import vn.brine.haileader.expolatorysearch.utils.DbpediaMusicConstant;
import vn.brine.haileader.expolatorysearch.utils.NetworkHelper;
import vn.brine.haileader.expolatorysearch.views.RecyclerItemClickListener;

public class MusicFragment extends Fragment implements View.OnClickListener,
        SlidingWindowAsynctask.SlidingWindowCallback, SearchResultAdapter.SearchResultAdapterCallback {

    public static final String TAG = MusicFragment.class.getCanonicalName();
    private static final String FILE_NAME = "assets/user_profile.rdf";

    private EditText mEdtSearch;
    private RecyclerView mRecyclerViewSearch;
    private TagView mTagGroupSearch;
    private RelativeLayout mRltSearchKeyword, mRltSearchExploratory;

    private SearchResultAdapter mSearchAdapter;

    private List<String> mListKeyword;
    private List<SearchResult> mListsearchResult;
    private List<String> mListUriToSearchExploratory;

    private boolean isNetworkAvaiable = true;

    public MusicFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        createUI(view);
        init();
        recyclerViewListener();
        tagViewListener();
        if(NetworkHelper.isInternetAvailable(getContext())) {
            isNetworkAvaiable = true;
        }else {
            isNetworkAvaiable = false;
            Toast.makeText(getContext(),"No Network connection available", Toast.LENGTH_SHORT).show();
        }
    }

    private void createUI(View view){
        mEdtSearch = (EditText)view.findViewById(R.id.edt_search);
        Button mBtnSearchKeyword = (Button) view.findViewById(R.id.btn_search_keyword);
        Button mBtnSearchExploratory = (Button) view.findViewById(R.id.btn_search_exploratory);
        mRecyclerViewSearch = (RecyclerView)view.findViewById(R.id.recycle_result);
        mTagGroupSearch = (TagView) view.findViewById(R.id.tag_group_uri);
        mRltSearchKeyword = (RelativeLayout) view.findViewById(R.id.rlt_search_keyword);
        mRltSearchExploratory = (RelativeLayout) view.findViewById(R.id.rlt_search_exploratory);

        mBtnSearchKeyword.setOnClickListener(this);
        mBtnSearchExploratory.setOnClickListener(this);
    }

    private void init(){
        mListKeyword = new ArrayList<>();
        mListsearchResult = new ArrayList<>();
        mListUriToSearchExploratory = new ArrayList<>();

        mSearchAdapter = new SearchResultAdapter(getContext(), mListsearchResult, this);

        mRecyclerViewSearch.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager =
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mRecyclerViewSearch.setLayoutManager(layoutManager);
        mRecyclerViewSearch.addItemDecoration(
                new DividerItemDecoration(getContext(), LinearLayout.VERTICAL));
        mRecyclerViewSearch.setItemAnimator(new DefaultItemAnimator());
        mRecyclerViewSearch.setAdapter(mSearchAdapter);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_search_keyword:
                String keywords = getKeywordInput();
                if(keywords != null){
                    analysisAndSearchQueryEntity(keywords);
                }
                break;
        }
    }

    private void recyclerViewListener(){
        mRecyclerViewSearch.addOnItemTouchListener(
                new RecyclerItemClickListener(getContext(), mRecyclerViewSearch,
                        new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        //TODO: onItemclick
                    }

                    @Override
                    public void onItemLongClick(View view, int position) {
                        //TODO: onItemlong click
                    }
                }));
    }

    private void tagViewListener(){
        mTagGroupSearch.setOnTagClickListener(new TagView.OnTagClickListener() {
            @Override
            public void onTagClick(Tag tag, int i) {
                showLogAndToast("onTagClick");
            }
        });

        mTagGroupSearch.setOnTagDeleteListener(new TagView.OnTagDeleteListener() {
            @Override
            public void onTagDeleted(TagView tagView, Tag tag, int i) {
                showLogAndToast("onTagDeleted");
                mTagGroupSearch.remove(i);
                if(canHideSearchExploratoryView()){
                    hideSearchExploratoryView();
                }
                mListUriToSearchExploratory.remove(i);
            }
        });

        mTagGroupSearch.setOnTagLongClickListener(new TagView.OnTagLongClickListener() {
            @Override
            public void onTagLongClick(Tag tag, int i) {
                showLogAndToast("onTagLongClick");
            }
        });
    }


    @Override
    public void detailsProfileOfUri(String uri) {
        Bundle arguments = new Bundle();
        arguments.putString(DetailsFragment.URI, uri);
        DetailsFragment detailsFragment = new DetailsFragment();
        detailsFragment.setArguments(arguments);
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.addToBackStack(null);
        ft.replace(R.id.containerView, detailsFragment, "DetailsFragment").commit();
    }

    @Override
    public void addUriToSearch(String uri) {
        showSearchExploratoryView();
        if(mListUriToSearchExploratory.contains(uri)){
            showLogAndToast("Added");
        }
        String[] list = uri.split("/");
        showLogAndToast(list[list.length - 1]);

        Tag tag = new Tag(list[list.length - 1]);
        tag.isDeletable = true;
        tag.radius = 10f;

        mListUriToSearchExploratory.add(uri);
        mTagGroupSearch.addTag(tag);
    }

    private boolean canHideSearchExploratoryView(){
        if(mTagGroupSearch.getTags().size() == 0){
            return true;
        }
        return false;
    }

    private void showSearchExploratoryView(){
        if(mRltSearchExploratory.getVisibility() == View.GONE){
            mRltSearchExploratory.setVisibility(View.VISIBLE);
            mRltSearchKeyword.setVisibility(View.GONE);
        }
    }

    private void hideSearchExploratoryView(){
        if(mRltSearchExploratory.getVisibility() == View.VISIBLE){
            mRltSearchExploratory.setVisibility(View.GONE);
            mRltSearchKeyword.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void searchExploratory(String uri) {

    }

    @Override
    public void onCompleteSlidingWindow(List<ResultSet> resultSets) {
        if(resultSets == null) return;
        for(ResultSet resultSet : resultSets){
            while (resultSet.hasNext()){
                QuerySolution querySolution = resultSet.nextSolution();
                if(querySolution == null) break;
                Literal label = querySolution.getLiteral("label");
                Resource uri = (Resource) querySolution.get("s");
                if(label != null && uri != null){
                    SearchResult searchResult =
                            new SearchResult(label.getString(), uri.toString());
                    updateResult(searchResult);
                }
            }
        }
    }

    private void updateResult(SearchResult result){
        mListsearchResult.add(result);
        mSearchAdapter.notifyDataSetChanged();
    }

    private String getKeywordInput(){
        String keywords = mEdtSearch.getText().toString().trim();
        if(keywords.length() == 0){
            showErrorEmptyKeyword();
            return null;
        }
        return keywords;
    }

    private void showErrorEmptyKeyword(){
        mEdtSearch.setError("Keyword cannot empty!");
    }

    private void analysisAndSearchQueryEntity(String keywords){
        splitTextSearchToPhrase(keywords);
        slidingWindowSearch();
    }

    private void splitTextSearchToPhrase(String keywords){
        mListKeyword.clear();

        String[] splitTextSearch = keywords.split(" ");
        int lengthListSplit = splitTextSearch.length;
        for (int i = 0; i < lengthListSplit; i++) {
            if (i + 2 < lengthListSplit) {
                String pharse = splitTextSearch[i] + " " +
                        splitTextSearch[i + 1] + " " + splitTextSearch[i + 2];
                mListKeyword.add(pharse);
            }
            if (i + 1 < lengthListSplit) {
                String pharse = splitTextSearch[i] + " " + splitTextSearch[i + 1];
                mListKeyword.add(pharse);
            }
            if (!splitTextSearch[i].equals("film")) {
                mListKeyword.add(splitTextSearch[i]);
            }
        }
        showLogAndToast(mListKeyword.toString());
    }

    private void slidingWindowSearch(){
        if(mListKeyword == null) return;
        mListsearchResult.clear();
        if(isNetworkAvaiable){
            new SlidingWindowAsynctask(getContext(), this).execute(mListKeyword);
        }else{
            showLogAndToast("No Network connection available");
        }
    }

//    private void loadUserInfo(){
//        Model model = FileManager.get().loadModel(FILE_NAME);
//        showLogAndToast(model.toString());
//        String queryString =
//                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
//                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
//                "PREFIX mo: <http://purl.org/ontology/mo/> " +
//                "SELECT * WHERE { " +
//                " ?p mo:MusicArtist ?interest ." +
//                "}";
//        Query query = QueryFactory.create(queryString);
//        QueryExecution qexec = QueryExecutionFactory.create(query, model);
//        try {
//            ResultSet results = qexec.execSelect();
//            while (results.hasNext()){
//                QuerySolution solution = results.nextSolution();
//                showLogAndToast(solution.toString());
//                Resource resource = (Resource) solution.get("interest");
//                showLogAndToast(resource.getURI());
//                //Literal name = solution.getLiteral("interest");
//                //showLogAndToast(name.getString());
//            }
//        } finally {
//            qexec.close();
//        }
//    }

    private void showLog(String message){
        Log.d(TAG, message);
    }

    private void showLogAndToast(final String message){
        showLog(message);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
