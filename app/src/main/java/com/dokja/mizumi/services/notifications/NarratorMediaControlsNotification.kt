package com.dokja.mizumi.services.notifications

//class NarratorMediaControlsNotification @Inject constructor(
//    @ApplicationContext private val context: Context,
//    private val notificationsCenter: NotificationsCenter,
//    private val readerManager: ReaderManager,
//) {
//    private val scope: CoroutineScope = CoroutineScope(
//        SupervisorJob() + Dispatchers.Main.immediate + CoroutineName("NarratorNotificationService")
//    )
//
//    private val channelName = context.getString(R.string.notification_channel_name_reader_narrator)
//    private val channelId = "Reader narrator"
//    private val mediaTagDebug = "NovelDokusha_narratorMediaControls"
//    val notificationId: Int = channelId.hashCode()
//
//    private var mediaSession: MediaSessionCompat? = null
//
//    fun handleCommand(intent: Intent?) {
//        MediaButtonReceiver.handleIntent(mediaSession, intent)
//    }
//
//    fun createNotificationMediaControls(context: Context): Notification? {
//
//        val readerSession = readerManager.session ?: return null
//
//        val mbrIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
//            context,
//            PlaybackStateCompat.ACTION_PLAY_PAUSE
//        )
//        val mediaSession = MediaSessionCompat(
//            context,
//            mediaTagDebug,
//            ComponentName(context, MediaButtonReceiver::class.java),
//            mbrIntent
//        ).also {
//            it.setCallback(NarratorMediaControlsCallback(readerSession.readerTextToSpeech))
//            mediaSession = it
//        }
//
//        val cancelButton = MediaButtonReceiver.buildMediaButtonPendingIntent(
//            context,
//            PlaybackStateCompat.ACTION_STOP
//        )
//
//        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
//            .setShowCancelButton(true)
//            .setShowActionsInCompactView(0, 2, 4)
//            .setCancelButtonIntent(cancelButton)
//            .setMediaSession(mediaSession.sessionToken)
//
//        val readerIntent = ReaderActivity.IntentData(
//            ctx = context,
//            bookUrl = readerSession.bookUrl,
//            chapterUrl = readerSession.currentChapter.chapterUrl,
//            scrollToSpeakingItem = true
//        )
//        readerIntent.setFlags(
//            Intent.FLAG_ACTIVITY_CLEAR_TOP
//                    or Intent.FLAG_ACTIVITY_SINGLE_TOP
//        )
//
//        val chain = listOf<Intent>(
//            Intent(context, MainActivity::class.java)
//                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
//            ChaptersActivity.IntentData(
//                context,
//                BookMetadata(
//                    url = readerSession.bookUrl,
//                    title = readerSession.bookTitle ?: ""
//                )
//            ),
//            readerIntent
//        )
//
//        fun generateIntentStack() = PendingIntent.getActivities(
//            context,
//            readerSession.bookUrl.hashCode(),
//            chain.toTypedArray(),
//            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
//        )
//
//        val actionIntentPrevious = NotificationCompat.Action(
//            R.drawable.ic_media_control_previous,
//            context.getString(R.string.media_control_previous),
//            MediaButtonReceiver.buildMediaButtonPendingIntent(
//                context,
//                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
//            )
//        )
//        val actionIntentRewind = NotificationCompat.Action(
//            R.drawable.ic_media_control_rewind,
//            context.getString(R.string.media_control_rewind),
//            MediaButtonReceiver.buildMediaButtonPendingIntent(
//                context, PlaybackStateCompat.ACTION_REWIND
//            )
//        )
//        val actionIntentPause = NotificationCompat.Action(
//            R.drawable.ic_media_control_pause,
//            context.getString(R.string.media_control_play),
//            MediaButtonReceiver.buildMediaButtonPendingIntent(
//                context, PlaybackStateCompat.ACTION_PAUSE
//            )
//        )
//        val actionIntentPlay = NotificationCompat.Action(
//            R.drawable.ic_media_control_play,
//            context.getString(R.string.media_control_pause),
//            MediaButtonReceiver.buildMediaButtonPendingIntent(
//                context, PlaybackStateCompat.ACTION_PLAY
//            )
//        )
//        val actionIntentFastForward = NotificationCompat.Action(
//            R.drawable.ic_media_control_fast_forward,
//            context.getString(R.string.media_control_fast_forward),
//            MediaButtonReceiver.buildMediaButtonPendingIntent(
//                context, PlaybackStateCompat.ACTION_FAST_FORWARD
//            )
//        )
//        val actionIntentNext = NotificationCompat.Action(
//            R.drawable.ic_media_control_next,
//            context.getString(R.string.media_control_next),
//            MediaButtonReceiver.buildMediaButtonPendingIntent(
//                context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT
//            )
//        )
//
//        fun NotificationCompat.Builder.defineActions(
//            isPlaying: Boolean
//        ) {
//            clearActions()
//            addAction(actionIntentPrevious)
//            addAction(actionIntentRewind)
//            if (isPlaying) {
//                addAction(actionIntentPause)
//            } else {
//                addAction(actionIntentPlay)
//            }
//            addAction(actionIntentFastForward)
//            addAction(actionIntentNext)
//        }
//
//        val notificationBuilder = notificationsCenter.showNotification(
//            channelId = channelId,
//            channelName = channelName,
//            notificationId = notificationId,
//            importance = NotificationManager.IMPORTANCE_LOW
//        ) {
//            title = ""
//            text = ""
//            defineActions(isPlaying = readerSession.readerTextToSpeech.state.isPlaying.value)
//            setOngoing(true)
//            setCategory(NotificationCompat.CATEGORY_TRANSPORT)
//            priority = NotificationCompat.PRIORITY_HIGH
//            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_logo))
//            setStyle(mediaStyle)
//            setDeleteIntent(cancelButton)
//            color = Color.CYAN
//            setContentIntent(generateIntentStack())
//        }
//
//        // Update reader speaking state
//        scope.launch {
//            snapshotFlow { readerSession.readerTextToSpeech.state.isPlaying.value }
//                .collectLatest { isPlaying ->
//                    notificationsCenter.modifyNotification(
//                        builder = notificationBuilder,
//                        notificationId = notificationId,
//                    ) {
//                        defineActions(isPlaying = isPlaying)
//                    }
//                }
//        }
//
//        // Update chapter notification title
//        scope.launch {
//            snapshotFlow { readerSession.readerTextToSpeech.currentTextPlaying.value }
//                .mapNotNull { readerSession.readerChaptersLoader.chaptersStats[it.itemPos.chapterUrl] }
//                .map { it.chapter.title }
//                .distinctUntilChanged()
//                .collectLatest { chapterTitle ->
//                    notificationsCenter.modifyNotification(
//                        builder = notificationBuilder,
//                        notificationId = notificationId,
//                    ) {
//                        title = chapterTitle
//                    }
//                }
//        }
//
//        // Update chapter notification intent
//        scope.launch {
//            snapshotFlow { readerSession.readerTextToSpeech.currentTextPlaying.value }
//                .mapNotNull { readerSession.readerChaptersLoader.chaptersStats[it.itemPos.chapterUrl] }
//                .map { it.chapter.url }
//                .distinctUntilChanged()
//                .collectLatest { chapterUrl ->
//                    readerIntent.chapterUrl = chapterUrl
//                    notificationsCenter.modifyNotification(
//                        builder = notificationBuilder,
//                        notificationId = notificationId,
//                    ) {
//                        setContentIntent(generateIntentStack())
//                    }
//                }
//        }
//
//
//        // Update chapter progress
//        scope.launch {
//            snapshotFlow { readerSession.speakerStats.value }
//                .filterNotNull()
//                .collectLatest {
//                    val chapterPos = context.getString(
//                        R.string.chapter_x_over_n,
//                        it.chapterIndex + 1,
//                        it.chapterCount
//                    )
//                    val progress = context.getString(
//                        R.string.progress_x_percentage,
//                        it.chapterReadPercentage()
//                    )
//                    notificationsCenter.modifyNotification(
//                        builder = notificationBuilder,
//                        notificationId = notificationId,
//                    ) {
//                        text = "$chapterPos  $progress"
//                    }
//                }
//        }
//
//        return notificationBuilder.build()
//    }
//
//    fun close() {
//        mediaSession?.release()
//        mediaSession = null
//        scope.cancel()
//    }
//}