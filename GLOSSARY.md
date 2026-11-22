# Glossary

Use these terms in code, issues and UI text.

## Library

**Song**
One audio item that the app can play. It has a title, one or more artists, a release and a duration.
_Avoid_: track, tune, item

**Library**
All songs that the app knows about: local songs and server songs.
_Avoid_: collection, catalog

**Local song**
A song from a file on the phone.
_Avoid_: device song, offline song

**Release**
A group of songs that an artist publishes together: an album, an EP or a single.
_Avoid_: record, disc, album (when you mean any release)

**Release type**
The kind of a release: Album, EP or Single. It comes from a tag in the files. A release without this tag has no release type.
_Avoid_: album type, format

**Album**
A full-length release. The release type that is not EP or Single.
_Avoid_: LP, full album

**EP**
A short release, longer than a single and shorter than an album.
_Avoid_: mini album

**Single**
A release with one main song, and sometimes a small number of other versions or songs.
_Avoid_: one-track album

**Album artist**
The artist of the full release. It can be different from the artist of one song on that release.
_Avoid_: main artist

**Favorite**
A song that the user marked with the heart. On a music server that supports it, the mark is the same on the server.
_Avoid_: liked, loved, starred

## Music servers

**Music server**
A self-hosted server that holds the user's music: Navidrome or another Subsonic server, or Jellyfin.
_Avoid_: cloud, remote, online source

**Server song**
A song that comes from a music server. The app streams it, or plays its offline copy.
_Avoid_: cloud song, remote song, online song

**Offline copy**
A copy of a server song that the app keeps on the phone, so the song plays without a network.
_Avoid_: download, cached song, offline download

## Playback

**Queue**
The ordered list of songs that the player plays next.
_Avoid_: up next, play list

**Transition**
How one song changes to the next song: no transition, fade out then fade in, or overlap.
_Avoid_: crossfade (one type of transition only), gap

## Playlists

**Playlist**
A list of songs in an order that the user sets. The user names it and can change it.
_Avoid_: mixtape, collection

**Smart playlist**
A playlist that the app fills from a rule, for example the songs played most.
_Avoid_: auto playlist, dynamic playlist

**Daily Mix**
A list of songs that the app makes again each day from the user's listening.
_Avoid_: daily playlist, mix

## Listening

**Lyrics**
The words of a song. Synced lyrics have a time for each line, so the app can show the line that plays now.
_Avoid_: text, subtitles

**Scrobble**
A record of one song that the user listened to, sent to ListenBrainz or a music server.
_Avoid_: listen, play event
