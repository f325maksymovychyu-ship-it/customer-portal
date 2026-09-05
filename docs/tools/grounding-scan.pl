#!/usr/bin/perl
# D3 grounding evidence. For every normative sentence in sections 3 and 4 that
# carries AC tags, extract the load-bearing tokens — numbers, CAPS identifiers,
# quoted strings, backticked terms, HTTP status codes — and check each appears in
# at least one cited criterion. A token that does not is a candidate for a
# "tagged but unsupported" finding, which the skill calls the sneakiest case
# because the tag is what stops the next reader from checking.
#
# Candidates only. Every hit needs reading before it becomes a finding.
#
#   perl docs/tools/grounding-scan.pl [spec-file ...]
use strict;
use warnings;
use utf8;
binmode STDOUT, ':encoding(UTF-8)';

my $SPECS = 'docs/specifications';
my @files = @ARGV;
unless (@files) {
    opendir my $d, $SPECS or die "$SPECS: $!";
    @files = map { "$SPECS/$_" } sort grep { /^US-\d+\.\d+-.*\.md$/ } readdir $d;
    closedir $d;
    @files = sort {
        my ($ae,$as) = $a =~ /US-(\d+)\.(\d+)/; my ($be,$bs) = $b =~ /US-(\d+)\.(\d+)/;
        $ae <=> $be || $as <=> $bs;
    } @files;
}

# words that carry no discriminating power
my %STOP = map { $_ => 1 } qw(
    the a an and or but not is are was were be been being of to in on at by for
    with from that this these those it its as if then when where which who whom
    respond responds response request required must may can will shall no any
    each every all one two both same other another such per via into out over
    under after before during while until unless because so than more most less
    least only also just even still yet own way well
);

sub tokens {
    my ($text) = @_;
    my %t;
    # numbers with optional unit
    $t{lc $1} = 1 while $text =~ /\b(\d[\d\s]*(?:\.\d+)?)\b/g;
    # CAPS identifiers and enum values
    $t{$1} = 1 while $text =~ /\b([A-Z][A-Z0-9_]{2,})\b/g;
    # backticked code terms
    while ($text =~ /`([^`]+)`/g) { my $v = $1; $v =~ s/^\W+|\W+$//g; $t{lc $v} = 1 if length $v > 2 }
    # double-quoted strings
    while ($text =~ /"([^"]{3,})"/g) { $t{lc $1} = 1 }
    # error slugs
    $t{lc $1} = 1 while $text =~ m{errors/([a-z-]+)}g;
    delete $t{$_} for grep { $STOP{$_} } keys %t;
    return \%t;
}

sub normalise {
    my ($s) = @_;
    $s = lc $s;
    $s =~ s/\s+/ /g;
    $s =~ s/[\s,]//g;
    return $s;
}

my $candidates = 0;
for my $path (@files) {
    open my $fh, '<:encoding(UTF-8)', $path or die "$path: $!";
    my @l = map { my $x = $_; $x =~ s/\r?\n$//; $x } <$fh>;
    close $fh;

    # criterion text, by id
    my (%ac, $cur, $in_ac, $in_fence);
    for my $line (@l) {
        if ($line =~ /^##\s+2\.\s+Acceptance Criteria/) { $in_ac = 1; next }
        if ($line =~ /^##\s+/ && $line !~ /^##\s+2\./) { $in_ac = 0 }
        next unless $in_ac;
        if ($line =~ /^\*\*([A-Z]{2}-AC\d+)\s+—\s+(.*?)\*\*/) { $cur = $1; $ac{$cur} = $2 . "\n"; next }
        if ($line =~ /^```/) { $in_fence = !$in_fence; next }
        $ac{$cur} .= $line . "\n" if $cur;
    }

    # normative paragraphs
    my ($in_norm, @para, @hits);
    my $flush = sub {
        return unless @para;
        my $text = join(' ', @para); @para = ();
        return if $text =~ /^\s*\|/;                # tables carry their own citations
        my @tags = $text =~ /\[([A-Z]{2}-AC\d+)\]/g;
        return unless @tags;
        $text =~ s/\[[A-Z]{2}-AC\d+\]//g;           # a tag is not evidence for itself
        $text =~ s/\x{00A7}\s*\d+(\.\d+)?//g;       # cross-section references
        $text =~ s/\bUS-\d+\.\d+\b//g;              # cross-story references
        $text =~ s/\b[A-Z]{2}-AC\d+\b//g;           # criterion names quoted in prose
        my $support = join(' ', map { $ac{$_} // '' } @tags);
        my %sup = map { normalise($_) => 1 } (%{ tokens($support) });
        my $norm_sup = normalise($support);
        my $tok = tokens($text);
        my @missing;
        for my $k (sort keys %$tok) {
            my $n = normalise($k);
            next if $n eq '';
            next if index($norm_sup, $n) >= 0;
            push @missing, $k;
        }
        return unless @missing;
        my $short = length($text) > 130 ? substr($text, 0, 130) . '…' : $text;
        push @hits, [ join(',', @tags), join('; ', @missing), $short ];
    };
    for my $line (@l) {
        if ($line =~ /^##\s+/) { $flush->(); $in_norm = ($line =~ /^##\s+(3\.|4\.)/) ? 1 : 0; next }
        next unless $in_norm;
        if ($line =~ /^\s*$/) { $flush->(); next }
        push @para, $line;
    }
    $flush->();

    next unless @hits;
    my $name = $path; $name =~ s{.*/}{};
    print "### $name\n";
    for my $h (@hits) {
        $candidates++;
        printf "  [%s] missing from cited AC: %s\n      %s\n", @$h;
    }
}

print "\n$candidates candidate(s) — each needs reading before it becomes a finding\n";
