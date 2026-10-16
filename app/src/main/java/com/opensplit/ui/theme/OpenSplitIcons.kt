package com.opensplit.ui.theme

/**
 * A Material Symbols Rounded glyph. [codepoint] indexes the variable font in
 * res/font/material_symbols_rounded.ttf (subset by scripts/subset_material_symbols.py);
 * [filled] selects the FILL=1 axis instance. Render with SymbolIcon.
 */
data class Symbol(val codepoint: Int, val filled: Boolean = false)

object OpenSplitIcons {
    // Navigation
    val Home                   = Symbol(0xe9b2) // home
    val Groups                 = Symbol(0xea21) // group
    val Friends                = Symbol(0xea21) // people
    val Analytics              = Symbol(0xe26b) // bar_chart
    val Activity               = Symbol(0xe7f5) // notifications
    val Account                = Symbol(0xf20b) // account_circle

    // Expense actions
    val AddExpense             = Symbol(0xe145) // add
    val EditExpense            = Symbol(0xf097) // edit
    val DeleteExpense          = Symbol(0xe92e) // delete
    val ReceiptScan            = Symbol(0xe5fa) // document_scanner

    // Categories (same icon on rows, chips, chart legends, everywhere)
    val CategoryFood           = Symbol(0xe56c) // restaurant
    val CategoryTransport      = Symbol(0xeff7) // directions_car
    val CategoryBills          = Symbol(0xe8b0) // receipt
    val CategoryShopping       = Symbol(0xe8cc) // shopping_cart
    val CategoryRent           = Symbol(0xe9b2) // home
    val CategoryHealth         = Symbol(0xe548) // local_hospital
    val CategoryTravel         = Symbol(0xe539) // flight
    val CategoryEntertainment  = Symbol(0xe404) // movie
    val CategoryEducation      = Symbol(0xe80c) // school
    val CategoryOther          = Symbol(0xe5d3) // more_horiz
    val CategoryGroceries      = Symbol(0xe8cc) // local_grocery_store
    val CategoryCoffee         = Symbol(0xeb44) // local_cafe
    val CategoryDrinks         = Symbol(0xe540) // local_bar
    val CategoryGifts          = Symbol(0xe8f6) // card_giftcard
    val CategoryFitness        = Symbol(0xeb43) // fitness_center
    val CategorySubscriptions  = Symbol(0xe064) // subscriptions
    val CategoryPets           = Symbol(0xe91d) // pets

    // Split types
    val SplitEqual             = Symbol(0xeaf6) // balance
    val SplitExact             = Symbol(0xeac7) // numbers
    val SplitPercent           = Symbol(0xeb58) // percent
    val SplitShares            = Symbol(0xf0da) // pie_chart
    val SplitItemized          = Symbol(0xe0ee) // list_alt

    // Settlement / balance
    val Settle                 = Symbol(0xebcb) // handshake
    val OwedToYou              = Symbol(0xe5db) // arrow_downward
    val YouOwe                 = Symbol(0xe5d8) // arrow_upward

    // General UI
    val Search                 = Symbol(0xef7a) // search
    val Filter                 = Symbol(0xe152) // filter_list
    val Sort                   = Symbol(0xe164) // sort
    val More                   = Symbol(0xe5d4) // more_vert
    val Share                  = Symbol(0xe80d) // share
    val Export                 = Symbol(0xf090) // file_download
    val Camera                 = Symbol(0xe412) // camera_alt
    val Contacts               = Symbol(0xe0ba) // contacts
    val Close                  = Symbol(0xe5cd) // close
    val Check                  = Symbol(0xe668) // check
    val Back                   = Symbol(0xe5c4) // arrow_back
    val ChevronRight           = Symbol(0xe5cc) // chevron_right
    val Invite                 = Symbol(0xea4d) // person_add
    val Leave                  = Symbol(0xe879) // exit_to_app
    val Dropdown               = Symbol(0xe5c5) // arrow_drop_down
    val Security               = Symbol(0xe899) // lock
    val Info                   = Symbol(0xe88e) // info
    val Theme                  = Symbol(0xe40a) // palette
    val Help                   = Symbol(0xe8fd) // help
    val Logout                 = Symbol(0xe9ba) // logout
    val Person                 = Symbol(0xf0d3) // person
    val Edit                   = Symbol(0xf097) // edit
    val Download               = Symbol(0xf090) // file_download
    val AddMember              = Symbol(0xea4d) // person_add
    val Copy                   = Symbol(0xe14d) // content_copy
    val Link                   = Symbol(0xe250) // link
    val Whatsapp               = Symbol(0xe0c9) // chat
    val Sms                    = Symbol(0xe24c) // comment
    val PinFilled              = Symbol(0xf10d, filled = true) // push_pin
    val PinOutline             = Symbol(0xf10d) // push_pin
    val SortFilter             = Symbol(0xe429) // tune
    val Notes                  = Symbol(0xe26c) // notes
    val AttachFile             = Symbol(0xe226) // attach_file
    val PictureAsPdf           = Symbol(0xe415) // picture_as_pdf
    val Pdf                    = Symbol(0xe415) // picture_as_pdf
    val Pending                = Symbol(0xe88b) // hourglass_empty
    val VisibilityOn           = Symbol(0xe8f4) // visibility
    val VisibilityOff          = Symbol(0xe8f5) // visibility_off
    val Success                = Symbol(0xf0be) // check_circle
    val ErrorIcon              = Symbol(0xf8b6) // error
    val DateRange              = Symbol(0xe916) // date_range
    val Refresh                = Symbol(0xe5d5) // refresh
    val ReceiptLong            = Symbol(0xef6e) // receipt_long
    val AutoAwesome            = Symbol(0xe65f) // auto_awesome
    val SmartToy               = Symbol(0xf06c) // smart_toy
    val Lightbulb              = Symbol(0xe90f) // lightbulb
    val Insights               = Symbol(0xf092) // insights
    val Autorenew              = Symbol(0xe863) // autorenew
    val Payments               = Symbol(0xef63) // payments
    val CallSplit              = Symbol(0xe0b6) // call_split
    val WifiOff                = Symbol(0xe648) // wifi_off
    val Verified               = Symbol(0xef76) // verified
    val Key                    = Symbol(0xe73c) // key
    val Archive                = Symbol(0xe1a1) // inventory_2
    val Unarchive              = Symbol(0xe169) // unarchive
    val DeleteForever          = Symbol(0xe92b) // delete_forever
    val PersonRemove           = Symbol(0xef66) // person_remove
    val PieChart               = Symbol(0xf0da) // pie_chart
    val DonutLarge             = Symbol(0xe917) // donut_large
    val TableChart             = Symbol(0xe265) // table_chart
    val PhotoLibrary           = Symbol(0xe413) // photo_library
    val FlashOn                = Symbol(0xe3e7) // flash_on
    val FlashOff               = Symbol(0xe3e6) // flash_off
    val Straighten             = Symbol(0xe41c) // straighten
    val Percent                = Symbol(0xeb58) // percent
    val Mic                    = Symbol(0xe31d) // mic
    val QrCode                 = Symbol(0xf206) // qr_code_scanner
    val PersonAdd              = Symbol(0xea4d) // person_add
    val Notification           = Symbol(0xe7f5) // notifications
    val Delete                 = Symbol(0xe92e) // delete
    val ArrowBack              = Symbol(0xe5c4) // arrow_back
    val History                = Symbol(0xe8b3) // history

    // Formerly direct material-icons references
    val Notifications          = Symbol(0xe7f5) // notifications
    val ArrowDownward          = Symbol(0xe5db) // arrow_downward
    val ArrowUpward            = Symbol(0xe5d8) // arrow_upward
    val Add                    = Symbol(0xe145) // add
    val Send                   = Symbol(0xe163) // send
    val ArrowForward           = Symbol(0xe5c8) // arrow_forward
    val ArrowForwardIos        = Symbol(0xe5e1) // arrow_forward_ios
    val Widgets                = Symbol(0xe1bd) // widgets
    val VpnKey                 = Symbol(0xe0da) // vpn_key
    val Visibility             = Symbol(0xe8f4) // visibility
    val Shield                 = Symbol(0xe9e0) // shield
    val SecurityShield         = Symbol(0xe32a) // security
    val Schedule               = Symbol(0xefd6) // schedule
    val SwapHoriz              = Symbol(0xe8d4) // swap_horiz
    val Star                   = Symbol(0xf09a) // star
    val Mail                   = Symbol(0xe159) // mail
    val Lock                   = Symbol(0xe899) // lock
    val LightMode              = Symbol(0xe518) // light_mode
    val DarkMode               = Symbol(0xe51c) // dark_mode
    val BrightnessAuto         = Symbol(0xe1ab) // brightness_auto
    val Layers                 = Symbol(0xe53b) // layers
    val Language               = Symbol(0xea07) // language
    val Inbox                  = Symbol(0xe156) // inbox
    val GroupAdd               = Symbol(0xe7f0) // group_add
    val Favorite               = Symbol(0xe87e, filled = true) // favorite
    val FavoriteBorder         = Symbol(0xe87e) // favorite
    val FileDownload           = Symbol(0xf090) // file_download
    val DeleteSweep            = Symbol(0xe16c) // delete_sweep
    val DeleteOutline          = Symbol(0xe92e) // delete
    val CurrencyExchange       = Symbol(0xeb70) // currency_exchange
    val Code                   = Symbol(0xe86f) // code
    val CloudDownload          = Symbol(0xe2c0) // cloud_download
    val CleaningServices       = Symbol(0xf0ff) // cleaning_services
    val CheckCircle            = Symbol(0xf0be) // check_circle
    val Chat                   = Symbol(0xe0c9) // chat
    val CameraAlt              = Symbol(0xe412) // camera_alt
    val Call                   = Symbol(0xf0d4) // call
    val CalendarToday          = Symbol(0xe935) // calendar_today
    val Calculate              = Symbol(0xea5f) // calculate
    val Bookmark               = Symbol(0xe8e7, filled = true) // bookmark
    val BookmarkBorder         = Symbol(0xe8e7) // bookmark
    val List                   = Symbol(0xe896) // list
    val Menu                   = Symbol(0xe5d2) // menu
    val DragIndicator          = Symbol(0xe945) // drag_indicator
    val Palette                = Symbol(0xe40a) // palette
    val Group                  = Symbol(0xea21) // group
    val People                 = Symbol(0xea21) // people
    val Receipt                = Symbol(0xe8b0) // receipt
    val Restaurant             = Symbol(0xe56c) // restaurant
    val AccountCircle          = Symbol(0xf20b) // account_circle
    val Handshake              = Symbol(0xebcb) // handshake
    val Tune                   = Symbol(0xe429) // tune
    val Settings               = Symbol(0xe8b8) // settings
    val Wallet                 = Symbol(0xe850) // account_balance_wallet
    val Celebration            = Symbol(0xea65) // celebration
    val Public                 = Symbol(0xe80b) // public
    val Warning                = Symbol(0xf083) // warning
    val PersonSearch           = Symbol(0xf106) // person_search
    val Description            = Symbol(0xe873) // description
    val DataObject             = Symbol(0xead3) // data_object
    val MoreHoriz              = Symbol(0xe5d3) // more_horiz
    val Done                   = Symbol(0xe876) // done
    val Email                  = Symbol(0xe159) // mail
    val Sync                   = Symbol(0xe627) // sync
    val Upload                 = Symbol(0xf09b) // upload
    val ContentCopy            = Symbol(0xe14d) // content_copy

}
