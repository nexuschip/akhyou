package dulleh.akhyou.Search;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import dulleh.akhyou.Models.Anime;
import dulleh.akhyou.Models.SearchProviders.AnimeRushSearchProvider;
import dulleh.akhyou.Models.SearchProviders.SearchProvider;
import dulleh.akhyou.Utils.Events.SearchEvent;
import dulleh.akhyou.Utils.Events.SnackbarEvent;
import dulleh.akhyou.Utils.GeneralUtils;
import nucleus.presenter.RxPresenter;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

public class SearchPresenter extends RxPresenter<SearchFragment> {
    public static int ANIMERUSH_SEARCH_PROVIDER = 0;

    public static List<Anime> searchResultsCache = new ArrayList<>(0);

    private Subscription subscription;
    private SearchProvider searchProvider;

    private String searchTerm;
    public boolean isRefreshing;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (searchProvider == null) {
            searchProvider = new AnimeRushSearchProvider();
        }

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().registerSticky(this);
        }

    }

    @Override
    protected void onTakeView(SearchFragment view) {
        super.onTakeView(view);
        view.updateRefreshing();

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().registerSticky(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        searchProvider = null;
        unsubscribe();
    }

    private void unsubscribe () {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

    public void onEvent (SearchEvent event) {
        this.searchTerm = event.searchTerm;
        search();
    }

    public void search () {
        isRefreshing = true;
        if (getView() != null) {
            getView().updateRefreshing();
        }

        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }

        subscription = Observable.defer(new Func0<Observable<List<Anime>>>() {
            @Override
            public Observable<List<Anime>> call() {
                return Observable.just(searchProvider.searchFor(searchTerm));
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.deliver())
                .subscribe(new Subscriber<List<Anime>>() {
                    @Override
                    public void onNext(List<Anime> animes) {
                        searchResultsCache = animes;
                        isRefreshing = false;
                        getView().updateSearchResults();
                        this.unsubscribe();
                    }

                    @Override
                    public void onCompleted() {
                        // should be using Observable.just() as onCompleted is never called
                        // and it only runs once.
                    }

                    @Override
                    public void onError(Throwable e) {
                        isRefreshing = false;
                        searchResultsCache.clear();
                        getView().updateSearchResults();
                        postError(e);
                        this.unsubscribe();
                    }
                });
    }

    public void postError (Throwable e) {
        e.printStackTrace();
        EventBus.getDefault().post(new SnackbarEvent(GeneralUtils.formatError(e)));
    }

    public void postSuccess () {
        EventBus.getDefault().post(new SnackbarEvent("SUCCESS"));
    }

}
