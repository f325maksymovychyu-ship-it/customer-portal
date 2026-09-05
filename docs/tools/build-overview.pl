#!/usr/bin/perl
# Builds docs/backlog-overview.html from the markdown files in docs/backlog/.
# Run from the repository root:  perl docs/tools/build-overview.pl
use strict;
use warnings;
use utf8;

my $BACKLOG = 'docs/backlog';
my $OUT     = 'docs/backlog-overview.html';

# ---------------------------------------------------------------- helpers ---
sub esc {
    my ($s) = @_;
    return '' unless defined $s;
    $s =~ s/&/&amp;/g; $s =~ s/</&lt;/g; $s =~ s/>/&gt;/g; $s =~ s/"/&quot;/g;
    return $s;
}

# inline markdown -> html, on already-escaped text
sub inline {
    my ($s) = @_;
    return '' unless defined $s;
    $s = esc($s);
    $s =~ s/`([^`]+)`/<code>$1<\/code>/g;
    $s =~ s/\*\*([^*]+)\*\*/<strong>$1<\/strong>/g;
    $s =~ s/(?<!\*)\*([^*]+)\*(?!\*)/<em>$1<\/em>/g;
    $s =~ s/\[([^\]]+)\]\(([^)]+)\)/<a href="$2">$1<\/a>/g;
    $s =~ s/\\\|/|/g;
    return $s;
}

# split a markdown table row on unescaped pipes
sub cells {
    my ($row) = @_;
    $row =~ s/^\s*\|//; $row =~ s/\|\s*$//;
    my @out; my $cur = '';
    my @ch = split //, $row;
    for (my $i = 0; $i < @ch; $i++) {
        if ($ch[$i] eq '\\' && $i + 1 < @ch && $ch[$i+1] eq '|') { $cur .= '\|'; $i++; next; }
        if ($ch[$i] eq '|') { push @out, $cur; $cur = ''; next; }
        $cur .= $ch[$i];
    }
    push @out, $cur;
    s/^\s+|\s+$//g for @out;
    return @out;
}

sub is_divider { my ($r) = @_; return $r =~ /^\s*\|[\s:|-]+\|\s*$/; }

# gherkin keyword highlighting
sub gherkin_html {
    my ($code) = @_;
    my @lines;
    for my $l (split /\n/, $code) {
        my $e = esc($l);
        if ($e =~ s/^(\s*)(Given|When|Then)\b/$1<span class="kw">$2<\/span>/) { }
        elsif ($e =~ s/^(\s*)(And|But|Because)\b/$1<span class="and">$2<\/span>/) { }
        push @lines, $e;
    }
    return join("\n", @lines);
}

# render a block of markdown lines (lists, tables, paragraphs, fences)
sub render_block {
    my ($lines_ref, %opt) = @_;
    my @l = @$lines_ref;
    my $html = '';
    my $i = 0;
    while ($i < @l) {
        my $line = $l[$i];

        if ($line =~ /^\s*$/) { $i++; next; }

        # fenced code
        if ($line =~ /^```(\w*)/) {
            my $lang = $1 || '';
            my @body; $i++;
            while ($i < @l && $l[$i] !~ /^```/) { push @body, $l[$i]; $i++; }
            $i++;
            my $code = join("\n", @body);
            if ($lang eq 'gherkin') {
                $html .= '<div class="gherkin"><pre>' . gherkin_html($code) . '</pre></div>';
            } else {
                $html .= '<div class="codeblock"><pre>' . esc($code) . '</pre></div>';
            }
            next;
        }

        # table
        if ($line =~ /^\s*\|/ && $i + 1 < @l && is_divider($l[$i+1])) {
            my @head = cells($line);
            $i += 2;
            my @rows;
            while ($i < @l && $l[$i] =~ /^\s*\|/) { push @rows, [ cells($l[$i]) ]; $i++; }
            $html .= '<div class="tablewrap"><table><thead><tr>';
            $html .= '<th>' . inline($_) . '</th>' for @head;
            $html .= '</tr></thead><tbody>';
            for my $r (@rows) {
                $html .= '<tr>';
                for my $c (@$r) {
                    my $v = inline($c);
                    $v =~ s{<code>\[gate\]</code>}{<span class="mk gate">gate</span>};
                    $v =~ s{<code>\[manual\]</code>}{<span class="mk manual">manual</span>};
                    $html .= '<td>' . $v . '</td>';
                }
                $html .= '</tr>';
            }
            $html .= '</tbody></table></div>';
            next;
        }

        # unordered list
        if ($line =~ /^\s*[-*]\s+/) {
            my @items;
            while ($i < @l && $l[$i] =~ /^\s*[-*]\s+(.*)$/) {
                my $t = $1; $i++;
                while ($i < @l && $l[$i] =~ /^\s{2,}(?![-*]\s)(\S.*)$/) { $t .= ' ' . $1; $i++; }
                push @items, $t;
            }
            $html .= '<ul>' . join('', map { '<li>' . inline($_) . '</li>' } @items) . '</ul>';
            next;
        }

        # ordered list
        if ($line =~ /^\s*\d+\.\s+/) {
            my @items;
            while ($i < @l && $l[$i] =~ /^\s*\d+\.\s+(.*)$/) {
                my $t = $1; $i++;
                while ($i < @l && $l[$i] =~ /^\s{2,}(?!\d+\.\s)(\S.*)$/) { $t .= ' ' . $1; $i++; }
                push @items, $t;
            }
            my $cls = $opt{questions} ? ' class="qlist"' : '';
            $html .= "<ol$cls>";
            for my $it (@items) {
                my $esc_flag = ($it =~ /Escalation/) ? ' class="escalation"' : '';
                $html .= "<li$esc_flag>" . inline($it) . '</li>';
            }
            $html .= '</ol>';
            next;
        }

        # blockquote
        if ($line =~ /^\s*>\s?(.*)$/) {
            my @body;
            while ($i < @l && $l[$i] =~ /^\s*>\s?(.*)$/) { push @body, $1; $i++; }
            $html .= '<blockquote class="note">' . inline(join(' ', @body)) . '</blockquote>';
            next;
        }

        # AC heading: **XX-ACn — Title**
        if ($line =~ /^\*\*([A-Z]{2}-AC\d+)\s+—\s+(.+?)\*\*\s*$/) {
            $html .= '<p class="ac-head"><span class="ac-id">' . esc($1) . '</span>' . inline($2) . '</p>';
            $i++; next;
        }

        # sub-heading inside a section
        if ($line =~ /^###\s+(.*)$/) {
            my $t = $1;
            my $cls = ($t =~ /happy/i) ? ' happy' : '';
            $html .= '<p class="group' . $cls . '">' . inline($t) . '</p>';
            $i++; next;
        }

        # paragraph
        my @para;
        while ($i < @l && $l[$i] !~ /^\s*$/ && $l[$i] !~ /^(```|\s*\||\s*[-*]\s|\s*\d+\.\s|\s*>|###\s)/
               && $l[$i] !~ /^\*\*[A-Z]{2}-AC\d+\s/) {
            push @para, $l[$i]; $i++;
        }
        if (@para) { $html .= '<p>' . inline(join(' ', @para)) . '</p>'; }
        else { $i++; }
    }
    return $html;
}

# ------------------------------------------------------------ parse story ---
sub parse_story {
    my ($path) = @_;
    open my $fh, '<:encoding(UTF-8)', $path or die "$path: $!";
    my @lines = map { my $x = $_; $x =~ s/\r?\n$//; $x } <$fh>;
    close $fh;

    my %s = (path => $path, sections => [], meta => {});
    my ($cur_name, @cur_body);

    for my $line (@lines) {
        if ($line =~ /^#\s+(.*)$/) {
            my $h = $1;
            if ($h =~ /^Epic\s+(\d+)\s+—\s+(.+?):\s+(.+)$/) {
                @s{qw(epic epic_name feature)} = ($1, $2, $3);
            } else { $s{feature} = $h; }
            next;
        }
        if ($line =~ /^\*\*(Story ID|AC prefix|Module|Project):\*\*\s*(.*)$/) {
            my ($k, $v) = ($1, $2);
            $v =~ s/^`|`$//g if $k eq 'AC prefix';
            $s{meta}{$k} = $v;
            next;
        }
        if ($line =~ /^##\s+(.*)$/) {
            push @{$s{sections}}, { name => $cur_name, body => [@cur_body] } if defined $cur_name;
            $cur_name = $1; @cur_body = ();
            next;
        }
        push @cur_body, $line if defined $cur_name;
    }
    push @{$s{sections}}, { name => $cur_name, body => [@cur_body] } if defined $cur_name;

    $s{ac_count} = 0;
    for my $sec (@{$s{sections}}) {
        $s{ac_count} += scalar grep { /^\*\*[A-Z]{2}-AC\d+\s/ } @{$sec->{body}};
    }
    ($s{id} = $s{meta}{'Story ID'} || '') =~ s/\s//g;
    return \%s;
}

# --------------------------------------------------------------- gather -----
opendir my $dh, $BACKLOG or die "$BACKLOG: $!";
my @files = sort grep { /^US-\d+\.\d+-.*\.md$/ } readdir $dh;
closedir $dh;

# numeric sort by epic.story
@files = sort {
    my ($ae, $as) = $a =~ /^US-(\d+)\.(\d+)/;
    my ($be, $bs) = $b =~ /^US-(\d+)\.(\d+)/;
    $ae <=> $be || $as <=> $bs;
} @files;

my @stories = map { parse_story("$BACKLOG/$_") } @files;

# counts
my $n_stories = scalar @stories;
my $n_ac = 0; $n_ac += $_->{ac_count} for @stories;
my (%epics, $n_gate, $n_manual, $n_escalation);
$n_gate = $n_manual = $n_escalation = 0;
for my $s (@stories) {
    push @{$epics{$s->{epic}}{stories}}, $s;
    $epics{$s->{epic}}{name} = $s->{epic_name};
    for my $sec (@{$s->{sections}}) {
        for my $l (@{$sec->{body}}) {
            $n_gate++   while $l =~ /`\[gate\]`/g;
            $n_manual++ while $l =~ /`\[manual\]`/g;
        }
        $n_escalation += scalar grep { /Escalation/ } @{$sec->{body}}
            if $sec->{name} =~ /Open Questions/;
    }
}

# README cross-cutting tables
open my $rfh, '<:encoding(UTF-8)', "$BACKLOG/README.md" or die $!;
my @rl = map { my $x = $_; $x =~ s/\r?\n$//; $x } <$rfh>;
close $rfh;
my %readme;
{
    my ($cur, @body);
    for my $line (@rl) {
        if ($line =~ /^##\s+(.*)$/) {
            $readme{$cur} = [@body] if defined $cur;
            $cur = $1; @body = (); next;
        }
        push @body, $line if defined $cur;
    }
    $readme{$cur} = [@body] if defined $cur;
}

# ------------------------------------------------------------------ emit ----
binmode STDOUT, ':encoding(UTF-8)';
open my $out, '>:encoding(UTF-8)', $OUT or die "$OUT: $!";

print $out <<"HEAD";
<title>Customer Portal Backlog</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Archivo:wdth,wght\@62..125,400..700&family=IBM+Plex+Mono:wght\@400;500;600&family=IBM+Plex+Sans:wght\@400;500;600&display=swap">
<style>
:root{
  --bg:#F6F8F8; --surface:#FFFFFF; --surface-2:#EDF1F2; --border:#DBE3E5; --border-strong:#C3CFD2;
  --text:#0F1C1F; --muted:#59696C; --accent:#0E5E68; --accent-soft:#DCEDEF;
  --happy:#2E6B45; --happy-bg:#E7F1EA;
  --warn:#A33A2B;  --warn-bg:#F7E9E6;
  --gate:#2E6B45;  --gate-bg:#E7F1EA;
  --manual:#8A6410;--manual-bg:#F5EEDC;
  --nfr:#574A86;   --nfr-bg:#ECEAF6;
  --shadow:0 1px 2px rgba(15,28,31,.05), 0 8px 24px -18px rgba(15,28,31,.35);
  --f-display:"Archivo","Helvetica Neue",Arial,sans-serif;
  --f-body:"IBM Plex Sans","Segoe UI",Arial,sans-serif;
  --f-mono:"IBM Plex Mono",ui-monospace,Consolas,monospace;
}
\@media (prefers-color-scheme: dark){
  :root:not([data-theme="light"]){
    --bg:#0B1113; --surface:#121A1D; --surface-2:#182225; --border:#243135; --border-strong:#33454A;
    --text:#E1EAEB; --muted:#94A6A9; --accent:#54B9C1; --accent-soft:#112E33;
    --happy:#7CC694; --happy-bg:#14251B;
    --warn:#E89283;  --warn-bg:#2A1A17;
    --gate:#7CC694;  --gate-bg:#14251B;
    --manual:#DCAF57;--manual-bg:#282013;
    --nfr:#ABA0DF;   --nfr-bg:#1F1C2C;
    --shadow:0 1px 2px rgba(0,0,0,.4), 0 8px 24px -18px rgba(0,0,0,.9);
  }
}
:root[data-theme="dark"]{
  --bg:#0B1113; --surface:#121A1D; --surface-2:#182225; --border:#243135; --border-strong:#33454A;
  --text:#E1EAEB; --muted:#94A6A9; --accent:#54B9C1; --accent-soft:#112E33;
  --happy:#7CC694; --happy-bg:#14251B;
  --warn:#E89283;  --warn-bg:#2A1A17;
  --gate:#7CC694;  --gate-bg:#14251B;
  --manual:#DCAF57;--manual-bg:#282013;
  --nfr:#ABA0DF;   --nfr-bg:#1F1C2C;
  --shadow:0 1px 2px rgba(0,0,0,.4), 0 8px 24px -18px rgba(0,0,0,.9);
}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--text);font-family:var(--f-body);font-size:16px;line-height:1.6;-webkit-font-smoothing:antialiased}
a{color:var(--accent);text-decoration:none} a:hover{text-decoration:underline}
a:focus-visible,summary:focus-visible{outline:2px solid var(--accent);outline-offset:2px;border-radius:2px}
.masthead{border-bottom:1px solid var(--border);background:var(--surface)}
.masthead-inner{max-width:1200px;margin:0 auto;padding:56px 32px 40px;display:flex;flex-direction:column;gap:18px}
.eyebrow{font-family:var(--f-mono);font-size:12px;font-weight:500;letter-spacing:.14em;text-transform:uppercase;color:var(--accent)}
h1{font-family:var(--f-display);font-variation-settings:"wdth" 108,"wght" 680;font-size:clamp(2.1rem,4.4vw,3.3rem);line-height:1.05;margin:0;letter-spacing:-.015em;text-wrap:balance}
.lede{margin:0;max-width:64ch;font-size:1.05rem;color:var(--muted)}
.metastrip{display:flex;flex-wrap:wrap;margin-top:10px;border:1px solid var(--border);border-radius:6px;overflow:hidden}
.metastrip div{flex:1 1 140px;padding:12px 16px;background:var(--surface-2);border-right:1px solid var(--border)}
.metastrip div:last-child{border-right:0}
.metastrip dt{font-family:var(--f-mono);font-size:11px;letter-spacing:.1em;text-transform:uppercase;color:var(--muted);margin:0 0 3px}
.metastrip dd{margin:0;font-family:var(--f-display);font-variation-settings:"wdth" 100,"wght" 620;font-size:1.22rem;font-variant-numeric:tabular-nums}
.shell{max-width:1200px;margin:0 auto;padding:0 32px 96px;display:grid;grid-template-columns:238px minmax(0,1fr);gap:56px;align-items:start}
nav.toc{position:sticky;top:0;max-height:100vh;overflow-y:auto;padding:40px 0;font-size:.85rem}
nav.toc h2{font-family:var(--f-mono);font-size:11px;font-weight:500;letter-spacing:.14em;text-transform:uppercase;color:var(--muted);margin:0 0 12px}
nav.toc ol{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:2px}
nav.toc .grp{font-family:var(--f-display);font-variation-settings:"wdth" 96,"wght" 640;font-size:.78rem;letter-spacing:.04em;text-transform:uppercase;margin:16px 0 6px;padding-top:12px;border-top:1px solid var(--border)}
nav.toc a{display:block;color:var(--muted);padding:3px 0;line-height:1.35}
nav.toc a:hover{color:var(--accent);text-decoration:none}
nav.toc code{font-family:var(--f-mono);font-size:.76rem;color:var(--accent);margin-right:6px}
main{padding:44px 0 0;min-width:0}
section{margin:0 0 64px;scroll-margin-top:24px}
.epic-head{border-top:3px solid var(--accent);padding-top:18px;margin-bottom:26px}
.epic-head .kicker{font-family:var(--f-mono);font-size:12px;letter-spacing:.12em;text-transform:uppercase;color:var(--accent);display:block;margin-bottom:8px}
h2.epic{font-family:var(--f-display);font-variation-settings:"wdth" 104,"wght" 660;font-size:clamp(1.6rem,3vw,2.1rem);line-height:1.1;margin:0 0 10px;letter-spacing:-.01em}
.epic-goal{margin:0;color:var(--muted);max-width:68ch}
h3.sec{font-family:var(--f-display);font-variation-settings:"wdth" 100,"wght" 640;font-size:1.3rem;margin:0 0 14px}
.panel{background:var(--surface);border:1px solid var(--border);border-radius:8px;padding:24px 28px;box-shadow:var(--shadow);margin-bottom:22px}
.panel p{max-width:70ch}
.panel.alert{border-left:3px solid var(--warn)}
.story{background:var(--surface);border:1px solid var(--border);border-radius:8px;padding:26px 28px 22px;margin:0 0 22px;box-shadow:var(--shadow);scroll-margin-top:24px}
.story-head{display:flex;gap:12px;align-items:baseline;flex-wrap:wrap;margin-bottom:12px}
.story-id{font-family:var(--f-mono);font-size:.78rem;font-weight:600;letter-spacing:.04em;color:var(--accent);background:var(--accent-soft);padding:3px 8px;border-radius:4px;white-space:nowrap}
.story-head h4{font-family:var(--f-display);font-variation-settings:"wdth" 100,"wght" 620;font-size:1.15rem;margin:0;line-height:1.25}
.chips{display:flex;flex-wrap:wrap;gap:6px;margin:0 0 16px}
.chip{font-family:var(--f-mono);font-size:.7rem;letter-spacing:.03em;color:var(--muted);border:1px solid var(--border-strong);padding:2px 7px;border-radius:20px;white-space:nowrap}
.chip b{color:var(--text);font-weight:600}
blockquote.us{margin:0 0 18px;padding:14px 18px;border-left:3px solid var(--accent);background:var(--surface-2);border-radius:0 6px 6px 0;font-size:1rem}
blockquote.us p{margin:0}
blockquote.note{margin:0 0 12px;padding:10px 16px;border-left:3px solid var(--manual);background:var(--manual-bg);border-radius:0 6px 6px 0;font-size:.92rem;color:var(--text)}
details.sub{border-top:1px solid var(--border);padding:12px 0 4px}
details.sub>summary{font-family:var(--f-mono);font-size:11px;letter-spacing:.13em;text-transform:uppercase;color:var(--muted);cursor:pointer;list-style:none;display:flex;align-items:center;gap:8px}
details.sub>summary::-webkit-details-marker{display:none}
details.sub>summary::before{content:"+";font-size:13px;color:var(--accent);width:12px;display:inline-block}
details.sub[open]>summary::before{content:"–"}
details.sub>summary:hover{color:var(--accent)}
.subbody{padding-top:10px}
.subbody>*:first-child{margin-top:0}
.subbody p{margin:0 0 10px;max-width:70ch}
.subbody ul,.subbody ol{margin:0 0 10px;padding-left:20px;max-width:70ch}
.subbody li{margin-bottom:5px}
.group{font-family:var(--f-mono);font-size:.68rem;font-weight:600;letter-spacing:.11em;text-transform:uppercase;color:var(--muted);margin:18px 0 10px;padding-bottom:5px;border-bottom:1px solid var(--border)}
.group.happy{color:var(--happy)}
.ac-head{margin:0 0 8px;font-weight:600;font-size:.96rem;line-height:1.35}
.ac-id{font-family:var(--f-mono);font-weight:600;font-size:.8rem;color:var(--accent);margin-right:8px}
.gherkin{margin:0 0 14px}
.gherkin pre,.codeblock pre{margin:0;font-family:var(--f-mono);font-size:.84rem;line-height:1.6;background:var(--surface-2);border:1px solid var(--border);border-radius:5px;padding:11px 14px;overflow-x:auto;white-space:pre-wrap;word-break:break-word}
.gherkin .kw{font-weight:600;color:var(--accent)}
.gherkin .and{font-weight:500;color:var(--muted)}
.codeblock{margin:0 0 12px}
.tablewrap{overflow-x:auto;border:1px solid var(--border);border-radius:6px;margin:0 0 14px}
table{border-collapse:collapse;width:100%;font-size:.88rem;min-width:480px;background:var(--surface)}
th,td{text-align:left;padding:9px 13px;border-bottom:1px solid var(--border);vertical-align:top}
th{font-family:var(--f-mono);font-size:.68rem;letter-spacing:.09em;text-transform:uppercase;color:var(--muted);font-weight:500;background:var(--surface-2);white-space:nowrap}
tr:last-child td{border-bottom:0}
code{font-family:var(--f-mono);font-size:.86em;background:var(--surface-2);border:1px solid var(--border);border-radius:3px;padding:0 4px}
.gherkin code,.codeblock code,th code{background:none;border:0;padding:0}
.mk{font-family:var(--f-mono);font-size:.68rem;font-weight:600;letter-spacing:.06em;text-transform:uppercase;padding:2px 7px;border-radius:3px;white-space:nowrap}
.mk.gate{color:var(--gate);background:var(--gate-bg)}
.mk.manual{color:var(--manual);background:var(--manual-bg)}
ol.qlist li.escalation{color:var(--warn)}
ol.qlist li.escalation strong{color:var(--warn)}
.nfrbody{border-left:3px solid var(--nfr);padding-left:14px}
footer{border-top:1px solid var(--border);margin-top:20px;padding-top:22px;font-size:.86rem;color:var(--muted)}
\@media (max-width:900px){
  .shell{grid-template-columns:1fr;gap:0;padding:0 20px 64px}
  nav.toc{position:static;max-height:none;padding:28px 0 0;border-bottom:1px solid var(--border)}
  .masthead-inner{padding:40px 20px 30px}
  main{padding-top:30px}
  .story,.panel{padding:20px 18px}
}
</style>
HEAD

# masthead
my $n_epics = scalar keys %epics;
print $out qq{<header class="masthead"><div class="masthead-inner">
<span class="eyebrow">Product Backlog · generated from docs/backlog</span>
<h1>Customer Portal Backlog</h1>
<p class="lede">Every story for authentication, administration, support and notifications, with acceptance criteria in Gherkin, the mechanism that proves each one, and the decisions still waiting on a human. Generated from the markdown sources, so this page and the repository cannot drift apart.</p>
<dl class="metastrip">
<div><dt>Epics</dt><dd>$n_epics</dd></div>
<div><dt>Stories</dt><dd>$n_stories</dd></div>
<div><dt>Acceptance criteria</dt><dd>$n_ac</dd></div>
<div><dt>Gated checks</dt><dd>$n_gate</dd></div>
<div><dt>Manual checks</dt><dd>$n_manual</dd></div>
<div><dt>Escalations</dt><dd>$n_escalation</dd></div>
</dl></div></header>\n};

# shell + toc
print $out qq{<div class="shell"><nav class="toc" aria-label="Contents"><h2>Contents</h2>
<ol><li><a href="#conventions">Conventions</a></li><li><a href="#lifecycle">Ticket lifecycle</a></li><li><a href="#classes">Notification classes</a></li><li><a href="#blocked">Blocked dependencies</a></li><li><a href="#order">Build order</a></li></ol>\n};
for my $e (sort { $a <=> $b } keys %epics) {
    print $out qq{<div class="grp">Epic $e · } . esc($epics{$e}{name}) . qq{</div><ol>\n};
    for my $s (@{$epics{$e}{stories}}) {
        my $anchor = lc($s->{id}); $anchor =~ s/\./-/g;
        print $out qq{<li><a href="#$anchor"><code>} . esc($s->{id}) . '</code>' . esc($s->{feature}) . qq{</a></li>\n};
    }
    print $out "</ol>\n";
}
print $out "</nav><main>\n";

# cross-cutting sections from README
my %xsec = (
    conventions => ['Conventions', 'Conventions', ''],
    lifecycle   => ['Ticket lifecycle (Epic 4)', 'Ticket lifecycle', ''],
    classes     => ['Notification classes (Epic 5)', 'Notification classes', ''],
    blocked     => ['Open dependencies requiring approval', 'Blocked dependencies', ''],
    order       => ['Suggested build order', 'Build order', 'Ordered so that each step unblocks the next.'],
);
for my $key (qw(conventions lifecycle classes blocked order)) {
    my ($src, $title, $sub) = @{$xsec{$key}};
    next unless $readme{$src};
    my $cls = ($key eq 'blocked') ? ' alert' : '';
    print $out qq{<section id="$key"><h3 class="sec">} . esc($title) . qq{</h3><div class="panel$cls">};
    print $out '<p>' . esc($sub) . '</p>' if length $sub;
    print $out render_block($readme{$src});
    print $out "</div></section>\n";
}

# stories, by epic
my %collapsed = map { $_ => 1 } (
    'Assumptions & Defaults (confirm or override)', 'In Scope', 'Out of Scope',
    'API Contract', 'Data Model Notes', 'Error Envelope (RFC 9457 `ProblemDetail`)',
    'Enforcement Matrix',
);
for my $e (sort { $a <=> $b } keys %epics) {
    my $ename = esc($epics{$e}{name});
    my $count = scalar @{$epics{$e}{stories}};
    print $out qq{<section id="epic-$e"><div class="epic-head"><span class="kicker">Epic $e</span>};
    print $out qq{<h2 class="epic">$ename</h2><p class="epic-goal">$count stories.</p></div>\n};

    for my $s (@{$epics{$e}{stories}}) {
        my $anchor = lc($s->{id}); $anchor =~ s/\./-/g;
        print $out qq{<article class="story" id="$anchor">};
        print $out qq{<div class="story-head"><span class="story-id">} . esc($s->{id}) . '</span>';
        print $out '<h4>' . esc($s->{feature}) . "</h4></div>\n";

        print $out '<div class="chips">';
        print $out '<span class="chip"><b>AC prefix:</b> ' . esc($s->{meta}{'AC prefix'} // '') . '</span>';
        my $mod = $s->{meta}{Module} // '';
        $mod =~ s/\*\*//g;
        print $out '<span class="chip"><b>Module:</b> ' . inline($mod) . '</span>';
        print $out '<span class="chip"><b>Criteria:</b> ' . $s->{ac_count} . '</span>';
        print $out "</div>\n";

        for my $sec (@{$s->{sections}}) {
            my $name = $sec->{name};
            my $body = $sec->{body};
            next unless grep { /\S/ } @$body;

            if ($name eq 'User Story') {
                my $t = join(' ', grep { /\S/ } @$body);
                print $out '<blockquote class="us"><p>' . inline($t) . "</p></blockquote>\n";
                next;
            }
            if ($name eq 'Acceptance Criteria') {
                print $out '<p class="group" style="margin-top:4px">Acceptance Criteria</p>';
                print $out render_block($body);
                next;
            }
            if ($name =~ /^Non-Functional/) {
                print $out qq{<details class="sub" open><summary>Non-functional &amp; security</summary><div class="subbody nfrbody">};
                print $out render_block($body) . "</div></details>\n";
                next;
            }
            if ($name eq 'Open Questions') {
                print $out qq{<details class="sub" open><summary>Open questions</summary><div class="subbody">};
                print $out render_block($body, questions => 1) . "</div></details>\n";
                next;
            }
            my $label = $name;
            $label =~ s/\s*\(confirm or override\)//;
            $label =~ s/\s*\(RFC 9457 `ProblemDetail`\)/ (RFC 9457)/;
            print $out qq{<details class="sub"><summary>} . esc($label) . qq{</summary><div class="subbody">};
            print $out render_block($body) . "</div></details>\n";
        }
        print $out "</article>\n";
    }
    print $out "</section>\n";
}

print $out qq{<footer>Generated from <code>docs/backlog/*.md</code> by <code>docs/tools/build-overview.pl</code>. Edit the markdown, re-run the script — never edit this page by hand. Story and criterion identifiers are the trace from requirement to test to code.</footer>\n};
print $out "</main></div>\n";
close $out;

print "wrote $OUT\n";
printf "epics %d · stories %d · criteria %d · gate %d · manual %d · escalations %d\n",
    $n_epics, $n_stories, $n_ac, $n_gate, $n_manual, $n_escalation;
