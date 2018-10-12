package ml.docilealligator.infinityforreddit;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

class FetchSubscribedThing {
    interface FetchSubscribedThingListener {
        void onFetchSubscribedThingSuccess(ArrayList<SubscribedSubredditData> subscribedSubredditData,
                                           ArrayList<SubscribedUserData> subscribedUserData,
                                           ArrayList<SubredditData> subredditData);
        void onFetchSubscribedThingFail();
    }

    static void fetchSubscribedThing(final Context context, final Retrofit retrofit, final String lastItem,
                                     final ArrayList<SubscribedSubredditData> subscribedSubredditData,
                                     final ArrayList<SubscribedUserData> subscribedUserData,
                                     final ArrayList<SubredditData> subredditData,
                                     final FetchSubscribedThingListener fetchSubscribedThingListener, final int refreshTime) {
        if(refreshTime < 0) {
            fetchSubscribedThingListener.onFetchSubscribedThingFail();
            return;
        }

        RedditAPI api = retrofit.create(RedditAPI.class);

        String accessToken = context.getSharedPreferences(SharedPreferencesUtils.AUTH_CODE_FILE_KEY, Context.MODE_PRIVATE)
                .getString(SharedPreferencesUtils.ACCESS_TOKEN_KEY, "");

        Call<String> subredditDataCall = api.getSubscribedThing(lastItem, RedditUtils.getOAuthHeader(accessToken));
        subredditDataCall.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if(response.isSuccessful()) {
                    ParseSubscribedThing.parseSubscribedSubreddits(response.body(), subscribedSubredditData,
                            subscribedUserData, subredditData,
                            new ParseSubscribedThing.ParseSubscribedSubredditsListener() {

                                @Override
                                public void onParseSubscribedSubredditsSuccess(ArrayList<SubscribedSubredditData> subscribedSubredditData,
                                                                               ArrayList<SubscribedUserData> subscribedUserData,
                                                                               ArrayList<SubredditData> subredditData,
                                                                               String lastItem) {
                                    if(lastItem.equals("null")) {
                                        fetchSubscribedThingListener.onFetchSubscribedThingSuccess(
                                                subscribedSubredditData, subscribedUserData, subredditData);
                                    } else {
                                        fetchSubscribedThing(context, retrofit, lastItem, subscribedSubredditData,
                                                subscribedUserData, subredditData,
                                                fetchSubscribedThingListener, refreshTime);
                                    }
                                }

                                @Override
                                public void onParseSubscribedSubredditsFail() {
                                    fetchSubscribedThingListener.onFetchSubscribedThingFail();
                                }
                            });
                } else if(response.code() == 401) {
                    RefreshAccessToken.refreshAccessToken(context, new RefreshAccessToken.RefreshAccessTokenListener() {
                        @Override
                        public void onRefreshAccessTokenSuccess() {
                            fetchSubscribedThing(context, retrofit, lastItem, subscribedSubredditData,
                                    subscribedUserData, subredditData, fetchSubscribedThingListener, refreshTime - 1);
                        }

                        @Override
                        public void onRefreshAccessTokenFail() {}
                    });
                } else {
                    Log.i("call failed", response.message());
                    fetchSubscribedThingListener.onFetchSubscribedThingFail();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.i("call failed", t.getMessage());
                fetchSubscribedThingListener.onFetchSubscribedThingFail();
            }
        });
    }
}
