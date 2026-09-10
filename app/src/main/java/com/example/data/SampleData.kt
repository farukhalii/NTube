package com.example.data

import com.example.model.QualityOption
import com.example.model.ShortItem
import com.example.model.VideoItem
import kotlin.random.Random

object SampleData {

    val videoQualityOptions = listOf(
        QualityOption("1080p Full HD (No Watermark)", "1920x1080 • Clean Ultra HD (No Logo)", "145.2 MB", 145200000L, isAudioOnly = false),
        QualityOption("720p HD (No Watermark)", "1280x720 • Clean HD (No Logo)", "72.8 MB", 72800000L, isAudioOnly = false),
        QualityOption("480p SD (Clean Video)", "854x480 • Standard Quality", "38.4 MB", 38400000L, isAudioOnly = false),
        QualityOption("360p Fast (Clean)", "640x360 • Fast Download", "21.6 MB", 21600000L, isAudioOnly = false),
        QualityOption("240p Fast", "426x240 (Data Saver)", "11.2 MB", 11200000L, isAudioOnly = false)
    )

    val musicQualityOptions = listOf(
        QualityOption("MP3 320 kbps", "Studio Audio (Highest Quality)", "9.8 MB", 9800000L, isAudioOnly = true),
        QualityOption("MP3 192 kbps", "Standard Audio (High Quality)", "6.2 MB", 6200000L, isAudioOnly = true),
        QualityOption("MP3 128 kbps", "Compact Audio (Fast Download)", "3.9 MB", 3900000L, isAudioOnly = true),
        QualityOption("M4A / AAC", "256 kbps (Crystal Clear)", "4.5 MB", 4500000L, isAudioOnly = true)
    )

    val qualityOptions = videoQualityOptions + musicQualityOptions

    fun getInitialVideos(): List<VideoItem> {
        return listOf(
            // --- YouTube Trending Videos ---
            VideoItem(
                id = "yt_001",
                title = "Coke Studio Season 15 - Blockbuster (Official Music Video)",
                channelName = "Coke Studio Pakistan",
                channelAvatarUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFFF0000,
                views = "48M views",
                uploadTime = "2 months ago",
                duration = "4:32",
                thumbnailUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Blockbuster Coke Studio",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                platform = "YouTube"
            ),
            VideoItem(
                id = "yt_002",
                title = "Kabhi Main Kabhi Tum - Episode 35 Mega Drama [Eng Sub] - ARY Digital",
                channelName = "ARY Digital HD",
                channelAvatarUrl = "https://images.unsplash.com/photo-1566492031773-4f4e44671857?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFFF0000,
                views = "24M views",
                uploadTime = "1 day ago",
                duration = "38:40",
                thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Kabhi Main Kabhi Tum",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                platform = "YouTube"
            ),
            VideoItem(
                id = "yt_003",
                title = "$1,000,000 Extreme Laser Tag in Abandoned City - MrBeast",
                channelName = "MrBeast",
                channelAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFFF0000,
                views = "112M views",
                uploadTime = "3 weeks ago",
                duration = "18:24",
                thumbnailUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Extreme Laser Tag Challenge",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                platform = "YouTube"
            ),
            VideoItem(
                id = "yt_004",
                title = "Pakistan vs India Final Over Thriller - Asia Cup Highlights",
                channelName = "PCB Official",
                channelAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFFF0000,
                views = "32M views",
                uploadTime = "4 days ago",
                duration = "12:15",
                thumbnailUrl = "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "PAK vs IND Thriller",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
                platform = "YouTube"
            ),
            VideoItem(
                id = "yt_005",
                title = "4K BBC Earth: The Secret Life of Big Cats in Serengeti",
                channelName = "BBC Earth",
                channelAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFFF0000,
                views = "19M views",
                uploadTime = "5 months ago",
                duration = "52:10",
                thumbnailUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Big Cats Serengeti 4K",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                platform = "YouTube"
            ),
            VideoItem(
                id = "yt_006",
                title = "T-Series Party Mashup 2026 - Non Stop Bollywood Dance",
                channelName = "T-Series",
                channelAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFFF0000,
                views = "65M views",
                uploadTime = "1 month ago",
                duration = "26:45",
                thumbnailUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Bollywood Dance Mashup",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                platform = "YouTube"
            ),

            // --- TikTok Viral Videos ---
            VideoItem(
                id = "tt_001",
                title = "Matushka Ultrafunk Dance Challenge 🔥 #fyp #tiktokviral #trending",
                channelName = "@sukuna.edits",
                channelAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFF00F2FE,
                views = "62M views",
                uploadTime = "12 hours ago",
                duration = "0:35",
                thumbnailUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "TikTok Dance Trend",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
                platform = "TikTok"
            ),
            VideoItem(
                id = "tt_002",
                title = "Khaby Lame shows how to open milk packet simply 😂 #learnfromkhaby",
                channelName = "@khaby.lame",
                channelAvatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFF00F2FE,
                views = "95M views",
                uploadTime = "1 day ago",
                duration = "0:22",
                thumbnailUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Khaby Lame Life Hack",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                platform = "TikTok"
            ),
            VideoItem(
                id = "tt_003",
                title = "Crispy Fried Cheese Burger ASMR Deep Fry #satisfying #foodtiktok",
                channelName = "@bayashi.tiktok",
                channelAvatarUrl = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFF00F2FE,
                views = "41M views",
                uploadTime = "8 hours ago",
                duration = "0:45",
                thumbnailUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Burger ASMR Crunch",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
                platform = "TikTok"
            ),
            VideoItem(
                id = "tt_004",
                title = "Wait till the end 😂 Cat gets scared of cucumber! #funny #catsoftiktok",
                channelName = "@funnyanimals_hub",
                channelAvatarUrl = "https://images.unsplash.com/photo-1548767797-d8c844163c4c?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFF00F2FE,
                views = "29M views",
                uploadTime = "1 day ago",
                duration = "0:28",
                thumbnailUrl = "https://images.unsplash.com/photo-1548767797-d8c844163c4c?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Cat Scared of Cucumber",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                platform = "TikTok"
            ),
            VideoItem(
                id = "tt_005",
                title = "Insane POV Parkour Rooftop Jump in Paris 🏃💨 #parkour #adrenaline",
                channelName = "@parkour_france",
                channelAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFF00F2FE,
                views = "53M views",
                uploadTime = "2 days ago",
                duration = "0:39",
                thumbnailUrl = "https://images.unsplash.com/photo-1517649763962-0c623266ddc0?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Rooftop Jump POV",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
                platform = "TikTok"
            ),
            VideoItem(
                id = "tt_006",
                title = "Brazilian Phonk Slowed + Reverb Dance Trend 🇧🇷 #phonk #dance",
                channelName = "@phonkvibes_tok",
                channelAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFF00F2FE,
                views = "37M views",
                uploadTime = "6 hours ago",
                duration = "0:30",
                thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Brazilian Phonk Trend",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                platform = "TikTok"
            ),

            // --- Instagram Reels ---
            VideoItem(
                id = "ig_001",
                title = "Shangrila Lake & Skardu Valley Drone Cinematic Reel 🌄 #explore #reels",
                channelName = "wanderlust_diaries",
                channelAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFE1306C,
                views = "14.2M views",
                uploadTime = "5 hours ago",
                duration = "0:45",
                thumbnailUrl = "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Skardu Drone Reel",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
                platform = "Instagram"
            ),
            VideoItem(
                id = "ig_002",
                title = "Authentic Neapolitan Pizza in 900-degree Wood Oven 🍕 #instafood",
                channelName = "chef_marcello",
                channelAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFE1306C,
                views = "8.9M views",
                uploadTime = "2 days ago",
                duration = "1:02",
                thumbnailUrl = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Wood Oven Pizza Reel",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                platform = "Instagram"
            ),
            VideoItem(
                id = "ig_003",
                title = "Rainy Morning in London Cafe - Aesthetic Lofi Reel ☕🌧️ #aesthetic",
                channelName = "cozy_london",
                channelAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFE1306C,
                views = "6.7M views",
                uploadTime = "1 day ago",
                duration = "0:34",
                thumbnailUrl = "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Rainy London Coffee",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                platform = "Instagram"
            ),
            VideoItem(
                id = "ig_004",
                title = "Men's Streetwear Outfit Ideas for Winter 2026 👟 #fashionreels",
                channelName = "drip_archive",
                channelAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFE1306C,
                views = "11.4M views",
                uploadTime = "3 days ago",
                duration = "0:29",
                thumbnailUrl = "https://images.unsplash.com/photo-1552374196-1ab2a1c593e8?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Streetwear Outfit Reel",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                platform = "Instagram"
            ),
            VideoItem(
                id = "ig_005",
                title = "Human Flag & Planche Calisthenics Masterclass 💪🔥 #gymmotivation",
                channelName = "fitness_beast",
                channelAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFE1306C,
                views = "9.1M views",
                uploadTime = "18 hours ago",
                duration = "0:42",
                thumbnailUrl = "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Calisthenics Planche",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                platform = "Instagram"
            ),
            VideoItem(
                id = "ig_006",
                title = "Golden Hour Desert Dune Bashing Dubai 🌅 #dubailife #travel",
                channelName = "dubai_visuals",
                channelAvatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFE1306C,
                views = "15.8M views",
                uploadTime = "4 days ago",
                duration = "0:51",
                thumbnailUrl = "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Dubai Desert Sunset",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                platform = "Instagram"
            ),

            // --- Facebook Watch Videos ---
            VideoItem(
                id = "fb_001",
                title = "Rescuing a Trapped Puppy in High Mountains - Emotional Story 🐶❤️",
                channelName = "Animal Planet Watch",
                channelAvatarUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFF1877F2,
                views = "33M views",
                uploadTime = "1 week ago",
                duration = "5:12",
                thumbnailUrl = "https://images.unsplash.com/photo-1584551246679-0daf3d275d0f?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Puppy Rescue Story",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
                platform = "Facebook"
            ),
            VideoItem(
                id = "fb_002",
                title = "Old Lahore Street Food - Making 1000 Crispy Sweet Jalebis 🍯",
                channelName = "Lahore Food Diaries",
                channelAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFF1877F2,
                views = "21M views",
                uploadTime = "3 days ago",
                duration = "8:35",
                thumbnailUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Crispy Jalebi Making",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                platform = "Facebook"
            ),
            VideoItem(
                id = "fb_003",
                title = "Handcrafted River Table with Blue Epoxy Resin - Satisfying Process",
                channelName = "Woodworking Craft Masters",
                channelAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFF1877F2,
                views = "18.5M views",
                uploadTime = "2 weeks ago",
                duration = "14:20",
                thumbnailUrl = "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Epoxy River Table",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                platform = "Facebook"
            ),
            VideoItem(
                id = "fb_004",
                title = "Judge Frank Caprio shows incredible kindness to 90-year old driver ❤️",
                channelName = "Caught In Providence",
                channelAvatarUrl = "https://images.unsplash.com/photo-1566492031773-4f4e44671857?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFF1877F2,
                views = "47M views",
                uploadTime = "5 days ago",
                duration = "6:18",
                thumbnailUrl = "https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Judge Caprio Kindness",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                platform = "Facebook"
            ),
            VideoItem(
                id = "fb_005",
                title = "1969 Ford Mustang Fastback Forgotten in Barn for 40 Years Restored",
                channelName = "Classic Car Restorations",
                channelAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFF1877F2,
                views = "28M views",
                uploadTime = "6 days ago",
                duration = "22:50",
                thumbnailUrl = "https://images.unsplash.com/photo-1584345604476-8ec5e12e42dd?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "1969 Mustang Restoration",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
                platform = "Facebook"
            ),
            VideoItem(
                id = "fb_006",
                title = "Pride of Lions defend waterhole in dramatic wildlife moment",
                channelName = "National Geographic Wild",
                channelAvatarUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFF1877F2,
                views = "39M views",
                uploadTime = "2 weeks ago",
                duration = "11:05",
                thumbnailUrl = "https://images.unsplash.com/photo-1546182990-dffeafbe841d?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Pride of Lions Battle",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                platform = "Facebook"
            ),

            // --- Snapchat Spotlight Videos ---
            VideoItem(
                id = "sc_001",
                title = "Unbelievable Street Basketball Half-Court Dunk! 🏀 Spotlight Viral",
                channelName = "SnapSpotlightDaily",
                channelAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFFFFC00,
                views = "15M views",
                uploadTime = "6 hours ago",
                duration = "0:30",
                thumbnailUrl = "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Insane Dunk Spotlight",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                platform = "Snapchat"
            ),
            VideoItem(
                id = "sc_002",
                title = "Slow Motion 360 Skateboard Kickflip over 6 stairs 🛹 #skate",
                channelName = "SkateSpotlight",
                channelAvatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFFFFC00,
                views = "12M views",
                uploadTime = "10 hours ago",
                duration = "0:25",
                thumbnailUrl = "https://images.unsplash.com/photo-1520045892732-304bc3ac5d8e?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Slow Mo Kickflip",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
                platform = "Snapchat"
            ),
            VideoItem(
                id = "sc_003",
                title = "Street Magician makes coin vanish in front of crowd 🪄 #magic",
                channelName = "MagicTricksDaily",
                channelAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFFFFC00,
                views = "18.2M views",
                uploadTime = "14 hours ago",
                duration = "0:35",
                thumbnailUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Vanish Coin Illusion",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                platform = "Snapchat"
            ),
            VideoItem(
                id = "sc_004",
                title = "Red Bull Extreme Mountain Bike Jump over canyon 🚵💨 #freeride",
                channelName = "ExtremeAdrenaline",
                channelAvatarUrl = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFFFFC00,
                views = "22M views",
                uploadTime = "1 day ago",
                duration = "0:44",
                thumbnailUrl = "https://images.unsplash.com/photo-1544161515-4ab6ce6db874?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Canyon Jump Spotlight",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
                platform = "Snapchat"
            ),
            VideoItem(
                id = "sc_005",
                title = "Golden Retriever puppy learns to swim for the first time 🐕🏊 #cute",
                channelName = "PuppySpotlight",
                channelAvatarUrl = "https://images.unsplash.com/photo-1548767797-d8c844163c4c?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFFFFC00,
                views = "16.4M views",
                uploadTime = "8 hours ago",
                duration = "0:28",
                thumbnailUrl = "https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Puppy Swimming Spotlight",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                platform = "Snapchat"
            ),
            VideoItem(
                id = "sc_006",
                title = "ASMR Cutting Rainbow Kinetic Sand Cubes 🏖️ #satisfying #asmr",
                channelName = "SatisfyingSnaps",
                channelAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = 0xFFFFFC00,
                views = "27M views",
                uploadTime = "12 hours ago",
                duration = "0:40",
                thumbnailUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800&auto=format&fit=crop&q=80",
                centerPlayTitle = "Kinetic Sand ASMR",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                platform = "Snapchat"
            )
        )
    }

    private val titles = listOf(
        "Coke Studio Sufi Beats 4K Live Performance",
        "Matushka Viral Dance Trend Compilation #tiktok",
        "POV Rooftop Parkour Escape in Downtown #viral",
        "4K Drone Flyover Northern Paradise Shangrila #reels",
        "Rescuing Cute Puppies from River Storm #facebook",
        "Slow Mo 360 Skateboard Stunt on Halfpipe #snapchat",
        "Judge Caprio Most Touching Story Ever #watch",
        "ASMR Crispy Cheese Burger Deep Fry #foodtiktok",
        "Insane Half-Court Basketball Buzzer Beater #spotlight",
        "Top 10 Bollywood Party Anthems 2026 #youtube",
        "Dubai Luxury Lifestyle Cinematic Reel #instagram",
        "Khaby Lame Common Sense Solution to Scissors #tiktok"
    )

    private val channels = listOf(
        "@coke_studio" to 0xFFFF0000,
        "@sukuna.edits" to 0xFF00F2FE,
        "wanderlust_diaries" to 0xFFE1306C,
        "Animal Planet Watch" to 0xFF1877F2,
        "SnapSpotlightDaily" to 0xFFFFFC00,
        "@khaby.lame" to 0xFF00F2FE,
        "T-Series Official" to 0xFFFF0000,
        "Woodworking Craft Masters" to 0xFF1877F2,
        "SkateSpotlight" to 0xFFFFFC00,
        "drip_archive" to 0xFFE1306C
    )

    private val thumbnailImages = listOf(
        "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&auto=format&fit=crop&q=80"
    )

    private val realStreamingUrls = listOf(
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    )

    private val platformList = listOf("YouTube", "TikTok", "Instagram", "Snapchat", "Facebook")

    fun generateMoreVideos(startIndex: Int, count: Int = 10): List<VideoItem> {
        return (0 until count).map { i ->
            val idx = startIndex + i
            val randomTitle = titles[Random.nextInt(titles.size)]
            val (channelName, color) = channels[Random.nextInt(channels.size)]
            val viewCount = (Random.nextInt(1, 95)).toString() + listOf("K", "M").random() + " views"
            val time = listOf("2 hours ago", "3 days ago", "2 weeks ago", "4 months ago", "1 year ago").random()
            val min = Random.nextInt(0, 15)
            val sec = String.format("%02d", Random.nextInt(5, 59))
            val duration = if (min == 0) "0:$sec" else "$min:$sec"
            val thumb = thumbnailImages[Random.nextInt(thumbnailImages.size)]
            val streamUrl = realStreamingUrls[idx % realStreamingUrls.size]
            val platform = platformList[idx % platformList.size]

            VideoItem(
                id = "gen_vid_$idx",
                title = "$randomTitle #$idx",
                channelName = channelName,
                channelAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
                channelAvatarColor = color,
                views = viewCount,
                uploadTime = time,
                duration = duration,
                thumbnailUrl = thumb,
                centerPlayTitle = "▶ ${randomTitle.take(24)}",
                videoUrl = streamUrl,
                platform = platform
            )
        }
    }

    fun getInitialShorts(): List<ShortItem> {
        return listOf(
            ShortItem(
                id = "short_001",
                title = "Wait for the bass drop! 🤯 Insane drift in Tokyo #shorts #drift #phonk",
                channelName = "TokyoDriftOfficial",
                channelAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
                likesCount = "1.8M",
                commentsCount = "14.2K",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                audioTitle = "Original Audio - Phonk Drift Brazil",
                gradientColors = listOf(0xFF8A2387, 0xFFE94057, 0xFFF27121)
            ),
            ShortItem(
                id = "short_002",
                title = "Cat discovers mirror for the first time 😂 you won't believe reaction! #funny #cute",
                channelName = "MeowWorld",
                channelAvatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150&auto=format&fit=crop&q=80",
                likesCount = "3.4M",
                commentsCount = "29.8K",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                audioTitle = "Funny Cat Theme - Comedy Sound",
                gradientColors = listOf(0xFF0F2027, 0xFF203A43, 0xFF2C5364)
            ),
            ShortItem(
                id = "short_003",
                title = "Superhuman Calisthenics Routine in 30 Seconds 💪🔥 #gym #motivation",
                channelName = "IronMindset",
                channelAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
                likesCount = "920K",
                commentsCount = "8.4K",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                audioTitle = "GigaChad Phonk Anthem",
                gradientColors = listOf(0xFF2C3E50, 0xFFFD746C, 0xFFFF9068)
            ),
            ShortItem(
                id = "short_004",
                title = "The most delicious 5-minute dessert you will ever make 🍫🤤 #food #recipe",
                channelName = "QuickChef",
                channelAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80",
                likesCount = "2.1M",
                commentsCount = "18.6K",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
                audioTitle = "Lofi Cooking Aesthetic Beat",
                gradientColors = listOf(0xFF141E30, 0xFF243B55, 0xFF4B79A1)
            )
        )
    }

    private val shortsTitles = listOf(
        "Top 3 mind-blowing facts you didn't know 🧠 #facts #shorts",
        "Insane basketball trick shot on the first try 🏀🔥 #sports",
        "How animations are made in studio 🎨✨ #art #creative",
        "Unbelievable guitar solo speed test 🎸⚡ #music #solo",
        "Cutting kinetic sand ASMR satisfying sound 🎧 #asmr #satisfying",
        "Secret smartphone tips you must turn on right now 📱 #tech",
        "Parkour roof jump POV - Don't look down! 🏃‍♂️💨 #adrenaline",
        "Drawing realistic 3D optical illusion on paper ✏️ #drawing"
    )

    fun generateMoreShorts(startIndex: Int, count: Int = 8): List<ShortItem> {
        val gradients = listOf(
            listOf(0xFF8A2387, 0xFFE94057, 0xFFF27121),
            listOf(0xFF12c2e9, 0xFFc471ed, 0xFFf64f59),
            listOf(0xFF0F2027, 0xFF203A43, 0xFF2C5364),
            listOf(0xFF3A1C71, 0xFFD76D77, 0xFFFFAF7B),
            listOf(0xFF134E5E, 0xFF71B280),
            listOf(0xFF654ea3, 0xFFeaafc8)
        )

        return (0 until count).map { i ->
            val idx = startIndex + i
            val randomTitle = shortsTitles[Random.nextInt(shortsTitles.size)]
            val (channel, _) = channels[Random.nextInt(channels.size)]
            val likes = "${Random.nextInt(100, 999)}K"
            val comments = "${Random.nextInt(1, 45)}.${Random.nextInt(1, 9)}K"

            ShortItem(
                id = "gen_short_$idx",
                title = "$randomTitle ($idx)",
                channelName = channel,
                channelAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
                likesCount = likes,
                commentsCount = comments,
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                audioTitle = "Viral Audio Trending • $channel",
                gradientColors = gradients[Random.nextInt(gradients.size)]
            )
        }
    }
}
